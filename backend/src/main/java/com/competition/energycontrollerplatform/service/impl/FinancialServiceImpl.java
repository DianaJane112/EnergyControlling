package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.RechargeOrder;
import com.competition.energycontrollerplatform.entity.RefundLog;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.RechargeOrderMapper;
import com.competition.energycontrollerplatform.mapper.RefundLogMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.FinancialService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 财务管理服务实现。
 * <p>
 * <b>核心规范：</b>
 * <ul>
 *   <li>所有金额字段使用 {@link BigDecimal}，严禁 float/double</li>
 *   <li>统计聚合在 MySQL 层通过 {@code DATE_FORMAT + SUM + GROUP BY} 完成，绝不捞回 JVM 遍历</li>
 *   <li>非 ADMIN 角色强制注入当前商户 ID 做数据隔离</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FinancialServiceImpl implements FinancialService {

    private static final Logger log = LoggerFactory.getLogger(FinancialServiceImpl.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RechargeOrderMapper rechargeOrderMapper;
    private final RefundLogMapper refundLogMapper;
    private final CustomerMapper customerMapper;

    // ──────────────────────── 充值明细分页 ────────────────────────

    @Override
    public IPage<DepositLogVO> listDepositLog(FinancialPageRequest request) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getIsDeleted, 0);

        // ── 权限隔离：非 ADMIN 强制过滤 ──
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(RechargeOrder::getCustomerId, effectiveCustomerId);
        }

        // 时段筛选
        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(RechargeOrder::getCreateTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(RechargeOrder::getCreateTime, parseDateTime(request.getEndTime()));
        }
        // 交易类型筛选
        if (request.getTradeType() != null) {
            wrapper.eq(RechargeOrder::getTradeType, request.getTradeType());
        }
        // 关键字搜索（订单号/商户名）
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(RechargeOrder::getOrderNo, request.getKeyword())
                    .or()
                    .like(RechargeOrder::getDbName, request.getKeyword()));
        }

        wrapper.orderByDesc(RechargeOrder::getCreateTime);

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<RechargeOrder> page = new Page<>(pageNum, pageSize);
        IPage<RechargeOrder> orderPage = rechargeOrderMapper.selectPage(page, wrapper);

        Map<Long, Customer> customerMap = buildCustomerMapForRecharge(orderPage.getRecords());
        return orderPage.convert(o -> toDepositLogVO(o, customerMap));
    }

    // ──────────────────────── 充值统计（SQL 聚合） ────────────────────────

    @Override
    public List<DepositSummaryVO> getDepositSummary(DepositSummaryRequest request) {
        // 权限隔离
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());

        String granularity = request.getGranularity() != null ? request.getGranularity().toUpperCase() : "MONTH";
        List<Map<String, Object>> rows;

        // 直接在 MySQL 层 SUM + GROUP BY，绝不捞回 JVM
        switch (granularity) {
            case "DAY":
                rows = rechargeOrderMapper.sumByDay(request.getStartDate(), request.getEndDate(), effectiveCustomerId);
                break;
            case "YEAR":
                rows = rechargeOrderMapper.sumByYear(request.getStartDate(), request.getEndDate(), effectiveCustomerId);
                break;
            case "MONTH":
            default:
                rows = rechargeOrderMapper.sumByMonth(request.getStartDate(), request.getEndDate(), effectiveCustomerId);
                break;
        }

        List<DepositSummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            DepositSummaryVO vo = new DepositSummaryVO();
            vo.setPeriod(toStringValue(row.get("period")));
            vo.setTotalAmount(toBigDecimal(row.get("totalAmount")));
            vo.setChargeCount(toLong(row.get("chargeCount")));
            vo.setCustomerCount(toLong(row.get("customerCount")));
            result.add(vo);
        }

        log.info("[充值统计] granularity={}, customerId={}, rows={}",
                granularity, effectiveCustomerId, result.size());
        return result;
    }

    // ──────────────────────── 退费明细分页 ────────────────────────

    @Override
    public IPage<RefundLogVO> listRefundLog(FinancialPageRequest request) {
        LambdaQueryWrapper<RefundLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefundLog::getIsDeleted, 0);

        // ── 权限隔离 ──
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(RefundLog::getCustomerId, effectiveCustomerId);
        }

        // 时段筛选
        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(RefundLog::getOperateTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(RefundLog::getOperateTime, parseDateTime(request.getEndTime()));
        }

        wrapper.orderByDesc(RefundLog::getOperateTime);

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<RefundLog> page = new Page<>(pageNum, pageSize);
        IPage<RefundLog> logPage = refundLogMapper.selectPage(page, wrapper);

        Map<Long, Customer> customerMap = buildCustomerMapForRefund(logPage.getRecords());
        return logPage.convert(l -> toRefundLogVO(l, customerMap));
    }

    // ──────────────────────── Excel 导出 ────────────────────────

    @Override
    public void exportDepositLog(HttpServletResponse response, FinancialPageRequest request) {
        // 1. 构建查询条件（与 listDepositLog 完全一致，但不分页）
        LambdaQueryWrapper<RechargeOrder> wrapper = buildDepositLogWrapper(request);
        wrapper.orderByDesc(RechargeOrder::getCreateTime);

        List<RechargeOrder> orders = rechargeOrderMapper.selectList(wrapper);
        Map<Long, Customer> customerMap = buildCustomerMapForRecharge(orders);
        List<DepositLogVO> voList = orders.stream()
                .map(o -> toDepositLogVO(o, customerMap))
                .collect(Collectors.toList());

        // 2. 设置 HTTP 响应头
        setExcelResponse(response, "充值明细导出.xlsx");

        // 3. 流式写入 Excel（EasyExcel 内部流式写入磁盘，不会 OOM）
        try {
            EasyExcel.write(response.getOutputStream(), DepositLogVO.class)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet("充值明细")
                    .doWrite(voList);
        } catch (IOException e) {
            log.error("[Excel导出] 充值明细导出失败", e);
            throw new RuntimeException("Excel 导出失败", e);
        }

        log.info("[Excel导出] 充值明细导出完成，共 {} 条记录", voList.size());
    }

    @Override
    public void exportDepositSummary(HttpServletResponse response, DepositSummaryRequest request) {
        // 1. 复用已有统计聚合方法
        List<DepositSummaryVO> summaryList = getDepositSummary(request);

        // 2. 设置 HTTP 响应头
        String granularity = request.getGranularity() != null ? request.getGranularity().toUpperCase() : "MONTH";
        String fileName = "充值统计_" + granularity + ".xlsx";
        setExcelResponse(response, fileName);

        // 3. 流式写入 Excel
        try {
            EasyExcel.write(response.getOutputStream(), DepositSummaryVO.class)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet("充值统计")
                    .doWrite(summaryList);
        } catch (IOException e) {
            log.error("[Excel导出] 充值统计导出失败", e);
            throw new RuntimeException("Excel 导出失败", e);
        }

        log.info("[Excel导出] 充值统计导出完成，granularity={}, 共 {} 行", granularity, summaryList.size());
    }

    // ──────────────────────── 打印缴费记录 ────────────────────────

    @Override
    public DepositLogVO getDepositLogById(Long id) {
        RechargeOrder order = rechargeOrderMapper.selectById(id);
        if (order == null || order.getIsDeleted() != null && order.getIsDeleted() == 1) {
            log.warn("[打印缴费记录] 订单不存在或已删除 id={}", id);
            return null;
        }

        // 权限隔离：非 ADMIN 仅可查看自身关联商户的充值记录
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null || !myCustomerId.equals(order.getCustomerId())) {
                log.warn("[打印缴费记录] 无权访问 id={}, 请求商户={}, 所属商户={}",
                        id, myCustomerId, order.getCustomerId());
                return null;
            }
        }

        return toDepositLogVO(order);
    }

    // ──────────────────────── 退费明细导出 ────────────────────────

    @Override
    public void exportRefundLog(HttpServletResponse response, FinancialPageRequest request) {
        // 1. 构建查询条件（与 listRefundLog 完全一致，但不分页）
        LambdaQueryWrapper<RefundLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefundLog::getIsDeleted, 0);

        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(RefundLog::getCustomerId, effectiveCustomerId);
        }

        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(RefundLog::getOperateTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(RefundLog::getOperateTime, parseDateTime(request.getEndTime()));
        }

        wrapper.orderByDesc(RefundLog::getOperateTime);

        List<RefundLog> logs = refundLogMapper.selectList(wrapper);
        Map<Long, Customer> customerMap = buildCustomerMapForRefund(logs);
        List<RefundLogVO> voList = logs.stream()
                .map(l -> toRefundLogVO(l, customerMap))
                .collect(Collectors.toList());

        // 2. 设置 HTTP 响应头
        setExcelResponse(response, "退费明细导出.xlsx");

        // 3. 流式写入 Excel
        try {
            EasyExcel.write(response.getOutputStream(), RefundLogVO.class)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet("退费明细")
                    .doWrite(voList);
        } catch (IOException e) {
            log.error("[Excel导出] 退费明细导出失败", e);
            throw new RuntimeException("Excel 导出失败", e);
        }

        log.info("[Excel导出] 退费明细导出完成，共 {} 条记录", voList.size());
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 权限隔离：解析目标商户 ID。
     * <p>ADMIN 可查全局（customerId=null），非 ADMIN 强制用自己的商户 ID。</p>
     */
    private Long resolveCustomerId(Long requestedCustomerId) {
        if (SecurityUserContext.isAdmin()) {
            return requestedCustomerId; // null = 全量, 非null = 指定商户
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null) {
            // 未关联商户的 OPERATOR 不可查看任何财务数据
            log.warn("[权限拒绝] 非 ADMIN 且未关联商户的用户尝试访问财务数据");
            // 返回一个不可能匹配的 ID，确保查询返回空集而非暴露全量数据
            return -1L;
        }
        return myCustomerId;
    }

    private DepositLogVO toDepositLogVO(RechargeOrder order) {
        Map<Long, Customer> cm = Map.of();
        if (order.getCustomerId() != null) {
            Customer c = customerMapper.selectById(order.getCustomerId());
            if (c != null) cm = Map.of(c.getId(), c);
        }
        return toDepositLogVO(order, cm);
    }

    private DepositLogVO toDepositLogVO(RechargeOrder order, Map<Long, Customer> customerMap) {
        DepositLogVO vo = new DepositLogVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCustomerId(order.getCustomerId());
        vo.setMeterId(order.getMeterId());
        vo.setDbNo(order.getDbNo());
        vo.setAmount(order.getAmount());
        vo.setPayAmount(order.getPayamount());
        vo.setRealAmount(order.getRealamount());
        vo.setBalance(order.getBalance());
        vo.setChargeType(order.getCharegetype());
        vo.setChargeTypeLabel(chargeTypeLabel(order.getCharegetype()));
        vo.setTradeType(order.getTradeType());
        vo.setTradeTypeLabel(tradeTypeLabel(order.getTradeType()));
        vo.setOperatorName(order.getOperatorName());
        vo.setUserName(order.getUserName());
        vo.setProName(order.getProName());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());

        if (order.getCustomerId() != null) {
            Customer c = customerMap.get(order.getCustomerId());
            if (c != null) {
                vo.setCustomerName(c.getCustomerName());
            }
        }

        return vo;
    }

    private RefundLogVO toRefundLogVO(RefundLog logEntity) {
        Map<Long, Customer> cm = Map.of();
        if (logEntity.getCustomerId() != null) {
            Customer c = customerMapper.selectById(logEntity.getCustomerId());
            if (c != null) cm = Map.of(c.getId(), c);
        }
        return toRefundLogVO(logEntity, cm);
    }

    private RefundLogVO toRefundLogVO(RefundLog logEntity, Map<Long, Customer> customerMap) {
        RefundLogVO vo = new RefundLogVO();
        vo.setId(logEntity.getId());
        vo.setFlowNo(logEntity.getFlowNo());
        vo.setCustomerId(logEntity.getCustomerId());
        vo.setMeterId(logEntity.getMeterId());
        vo.setBalanceBefore(logEntity.getBalanceBefore());
        vo.setRefundAmount(logEntity.getRefundAmount());
        vo.setBalanceAfter(logEntity.getBalanceAfter());
        vo.setRefundReason(logEntity.getRefundReason());
        vo.setOperatorName(logEntity.getOperatorName());
        vo.setOperateTime(logEntity.getOperateTime());
        vo.setRemark(logEntity.getRemark());

        if (logEntity.getCustomerId() != null) {
            Customer c = customerMap.get(logEntity.getCustomerId());
            if (c != null) {
                vo.setCustomerName(c.getCustomerName());
            }
        }

        return vo;
    }

    private Map<Long, Customer> buildCustomerMapForRecharge(List<RechargeOrder> orders) {
        Set<Long> ids = orders.stream()
                .map(RechargeOrder::getCustomerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }

    private Map<Long, Customer> buildCustomerMapForRefund(List<RefundLog> logs) {
        Set<Long> ids = logs.stream()
                .map(RefundLog::getCustomerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }

    private LocalDateTime parseDateTime(String str) {
        try {
            return LocalDateTime.parse(str, DTF);
        } catch (Exception e) {
            // 尝试只解析日期
            try {
                return LocalDateTime.parse(str + " 00:00:00", DTF);
            } catch (Exception ex) {
                log.warn("时间解析失败: {}", str);
                return LocalDateTime.now();
            }
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Double d) return BigDecimal.valueOf(d);
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String toStringValue(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof java.sql.Date sd) return sd.toLocalDate().toString();
        if (value instanceof java.time.LocalDate ld) return ld.toString();
        if (value instanceof java.time.LocalDateTime ldt) return ldt.toLocalDate().toString();
        return value.toString();
    }

    /**
     * 构建充值明细查询条件（与 listDepositLog 共用，避免重复代码）。
     */
    private LambdaQueryWrapper<RechargeOrder> buildDepositLogWrapper(FinancialPageRequest request) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getIsDeleted, 0);

        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(RechargeOrder::getCustomerId, effectiveCustomerId);
        }

        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(RechargeOrder::getCreateTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(RechargeOrder::getCreateTime, parseDateTime(request.getEndTime()));
        }
        if (request.getTradeType() != null) {
            wrapper.eq(RechargeOrder::getTradeType, request.getTradeType());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(RechargeOrder::getOrderNo, request.getKeyword())
                    .or()
                    .like(RechargeOrder::getDbName, request.getKeyword()));
        }

        return wrapper;
    }

    /**
     * 收款类型编码 → 中文名称。
     */
    private String chargeTypeLabel(Integer chargeType) {
        if (chargeType == null) return "未知";
        return switch (chargeType) {
            case 1 -> "在线充值";
            case 2 -> "人工充值";
            case 3 -> "银行转账";
            default -> "其他(" + chargeType + ")";
        };
    }

    /**
     * 交易类型编码 → 中文名称。
     */
    private String tradeTypeLabel(Integer tradeType) {
        if (tradeType == null) return "未知";
        return switch (tradeType) {
            case 1 -> "充值电费";
            case 2 -> "充值水费";
            default -> "其他(" + tradeType + ")";
        };
    }

    /**
     * 设置 Excel 文件下载响应头。
     */
    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }
}
