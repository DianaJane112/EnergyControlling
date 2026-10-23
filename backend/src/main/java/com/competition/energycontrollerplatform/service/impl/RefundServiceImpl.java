package com.competition.energycontrollerplatform.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.enums.MeterType;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.RefundService;
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
import java.util.List;

/**
 * 退费服务实现。
 * <p>
 * 提供退费明细分页、详情、预览、导出功能。
 * <p>
 * <b>核心规范：</b>
 * <ul>
 *   <li>所有金额字段使用 {@link BigDecimal}</li>
 *   <li>非 ADMIN 角色强制隔离</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RefundDetailMapper refundDetailMapper;
    private final RefundLogMapper refundLogMapper;
    private final CustomerMapper customerMapper;
    private final MeterMapper meterMapper;
    private final PriceStrategyMapper priceStrategyMapper;

    // ──────────────────────── 退费明细分页 ────────────────────────

    @Override
    public IPage<RefundDetailVO> listRefundDetails(FinancialPageRequest request) {
        LambdaQueryWrapper<RefundDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefundDetail::getIsDeleted, 0);

        // ── 权限隔离 ──
        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(RefundDetail::getCustomerId, effectiveCustomerId);
        }

        // 时段筛选
        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(RefundDetail::getRefundTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(RefundDetail::getRefundTime, parseDateTime(request.getEndTime()));
        }

        // 关键字搜索（退费编号/原因/操作员）
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword();
            wrapper.and(w -> w
                    .like(RefundDetail::getRefundNo, keyword)
                    .or()
                    .like(RefundDetail::getRefundReason, keyword)
                    .or()
                    .like(RefundDetail::getOperatorName, keyword));
        }

        wrapper.orderByDesc(RefundDetail::getRefundTime);

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<RefundDetail> page = new Page<>(pageNum, pageSize);
        IPage<RefundDetail> result = refundDetailMapper.selectPage(page, wrapper);

        java.util.List<RefundDetail> entities = result.getRecords();
        java.util.Map<Long, Customer> customerMap = buildCustomerMap(entities);
        java.util.Map<Long, Meter> meterMap = buildMeterMap(entities);

        return result.convert(entity -> toVO(entity, customerMap, meterMap));
    }

    // ──────────────────────── 退费详情 ────────────────────────

    @Override
    public RefundDetailVO getRefundDetailById(Long id) {
        RefundDetail entity = refundDetailMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() != null && entity.getIsDeleted() == 1) {
            return null;
        }

        // 权限隔离
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null || !myCustomerId.equals(entity.getCustomerId())) {
                log.warn("[权限拒绝] 非ADMIN尝试查看其他商户的退费记录 id={}", id);
                return null;
            }
        }

        return toVO(entity);
    }

    /**
     * 单条记录的 VO 转换（无预加载，适用于 getRefundDetailById 等单条查询场景）。
     */
    private RefundDetailVO toVO(RefundDetail entity) {
        java.util.Map<Long, Customer> cm = entity.getCustomerId() != null
                ? customerMapper.selectBatchIds(java.util.List.of(entity.getCustomerId())).stream()
                    .collect(java.util.stream.Collectors.toMap(Customer::getId, c -> c, (a, b) -> a))
                : java.util.Map.of();
        java.util.Map<Long, Meter> mm = entity.getMeterId() != null
                ? meterMapper.selectBatchIds(java.util.List.of(entity.getMeterId())).stream()
                    .collect(java.util.stream.Collectors.toMap(Meter::getId, m -> m, (a, b) -> a))
                : java.util.Map.of();
        return toVO(entity, cm, mm);
    }

    // ──────────────────────── 退费预览 ────────────────────────

    @Override
    public RefundPreviewVO previewRefund(RefundPreviewRequest request) {
        Long meterId = request.getMeterId();
        BigDecimal refundAmount = request.getRefundAmount();

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(400, "退费金额必须大于0");
        }

        // 1. 查询表计信息
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null) {
            throw BusinessException.of(404, "表计不存在");
        }

        // 2. 查询商户信息
        Long customerId = meter.getCustomerId();
        if (customerId == null) {
            throw BusinessException.of(400, "表计未绑定商户，无法退费");
        }

        // 权限检查
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null || !myCustomerId.equals(customerId)) {
                throw BusinessException.of(403, "无权查看该表计信息");
            }
        }

        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw BusinessException.of(404, "关联商户不存在");
        }

        BigDecimal currentBalance = customer.getBalance() != null ? customer.getBalance() : BigDecimal.ZERO;

        // 余额不足检查
        if (currentBalance.compareTo(refundAmount) < 0) {
            throw BusinessException.of(400, "商户余额不足：当前余额 " + currentBalance + " 元，退费金额 " + refundAmount + " 元");
        }

        // 3. 构建预览响应
        RefundPreviewVO vo = new RefundPreviewVO();

        // 表计信息
        vo.setMeterId(meter.getId());
        vo.setDbNo(meter.getDbNo());
        vo.setMeterAddress(meter.getAddress() != null ? meter.getAddress() : meter.getMeteraddress());
        vo.setMeterType(meter.getMeterType());
        vo.setMeterTypeName(getMeterTypeName(meter.getMeterType()));
        vo.setMeterStatus(meter.getStatus());
        vo.setMeterStatusName(getMeterStatusName(meter.getStatus()));

        // 商户信息
        vo.setCustomerId(customer.getId());
        vo.setCustomerName(customer.getCustomerName());
        vo.setCustomerPhone(customer.getPhone());

        // 余额信息
        vo.setCurrentBalance(currentBalance);
        vo.setWarnline(meter.getWarnline());
        vo.setCredit(meter.getCredit());

        // 电价信息
        BigDecimal currentPrice = getEffectivePrice(meter);
        vo.setCurrentPrice(currentPrice);
        vo.setPriceStrategyName(getPriceStrategyName(meter.getRateConfigId()));

        // 退费预览
        vo.setRefundAmount(refundAmount);
        BigDecimal balanceAfterRefund = currentBalance.subtract(refundAmount);
        vo.setBalanceAfterRefund(balanceAfterRefund);

        // 退费后是否触发欠费停机（余额 <= 0 且无透支额度）
        boolean willShutdown = balanceAfterRefund.compareTo(BigDecimal.ZERO) <= 0
                && (meter.getCredit() == null || meter.getCredit().compareTo(BigDecimal.ZERO) <= 0);
        vo.setWillTriggerShutdown(willShutdown);

        vo.setPreviewTime(LocalDateTime.now());

        log.info("[退费预览] meterId={}, customerId={}, currentBalance={}, refundAmount={}, balanceAfter={}, willShutdown={}",
                meterId, customerId, currentBalance, refundAmount, balanceAfterRefund, willShutdown);

        return vo;
    }

    // ──────────────────────── Excel 导出 ────────────────────────

    @Override
    public void exportRefundDetails(HttpServletResponse response, FinancialPageRequest request) {
        LambdaQueryWrapper<RefundDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefundDetail::getIsDeleted, 0);

        Long effectiveCustomerId = resolveCustomerId(request.getCustomerId());
        if (effectiveCustomerId != null) {
            wrapper.eq(RefundDetail::getCustomerId, effectiveCustomerId);
        }

        if (StringUtils.hasText(request.getStartTime())) {
            wrapper.ge(RefundDetail::getRefundTime, parseDateTime(request.getStartTime()));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            wrapper.le(RefundDetail::getRefundTime, parseDateTime(request.getEndTime()));
        }

        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword();
            wrapper.and(w -> w
                    .like(RefundDetail::getRefundNo, keyword)
                    .or()
                    .like(RefundDetail::getRefundReason, keyword)
                    .or()
                    .like(RefundDetail::getOperatorName, keyword));
        }

        wrapper.orderByDesc(RefundDetail::getRefundTime);

        List<RefundDetail> entities = refundDetailMapper.selectList(wrapper);
        java.util.Map<Long, Customer> customerMap = buildCustomerMap(entities);
        java.util.Map<Long, Meter> meterMap = buildMeterMap(entities);
        List<RefundDetailVO> voList = entities.stream()
                .map(entity -> toVO(entity, customerMap, meterMap))
                .collect(java.util.stream.Collectors.toList());

        setExcelResponse(response, "退费明细导出.xlsx");

        try {
            EasyExcel.write(response.getOutputStream(), RefundDetailVO.class)
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

    private Long resolveCustomerId(Long requestedCustomerId) {
        if (SecurityUserContext.isAdmin()) {
            return requestedCustomerId;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null) {
            log.warn("[权限拒绝] 非 ADMIN 且未关联商户的用户尝试访问退费数据");
            return null;
        }
        return myCustomerId;
    }

    private RefundDetailVO toVO(RefundDetail entity,
                                java.util.Map<Long, Customer> customerMap,
                                java.util.Map<Long, Meter> meterMap) {
        RefundDetailVO vo = new RefundDetailVO();
        vo.setId(entity.getId());
        vo.setRefundNo(entity.getRefundNo());
        vo.setCustomerId(entity.getCustomerId());
        vo.setMeterId(entity.getMeterId());
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setRefundType(entity.getRefundType());
        vo.setRefundTypeLabel(refundTypeLabel(entity.getRefundType()));
        vo.setRefundReason(entity.getRefundReason());
        vo.setOriginalOrderId(entity.getOriginalOrderId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setRefundTime(entity.getRefundTime());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());

        // 填充商户名称（从预加载 Map）
        if (entity.getCustomerId() != null) {
            Customer c = customerMap.get(entity.getCustomerId());
            if (c != null) {
                vo.setCustomerName(c.getCustomerName());
            }
        }

        // 填充表号（从预加载 Map）
        if (entity.getMeterId() != null) {
            Meter m = meterMap.get(entity.getMeterId());
            if (m != null) {
                vo.setDbNo(m.getDbNo());
            }
        }

        return vo;
    }

    private java.util.Map<Long, Customer> buildCustomerMap(java.util.List<RefundDetail> entities) {
        java.util.Set<Long> ids = entities.stream()
                .map(RefundDetail::getCustomerId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return java.util.Map.of();
        return customerMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Customer::getId, c -> c, (a, b) -> a));
    }

    private java.util.Map<Long, Meter> buildMeterMap(java.util.List<RefundDetail> entities) {
        java.util.Set<Long> ids = entities.stream()
                .map(RefundDetail::getMeterId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return java.util.Map.of();
        return meterMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Meter::getId, m -> m, (a, b) -> a));
    }

    private String refundTypeLabel(String type) {
        if (type == null) return "未知";
        return switch (type.toUpperCase()) {
            case "PARTIAL_REFUND" -> "部分退费";
            case "FULL_REFUND" -> "全额退费";
            default -> type;
        };
    }

    /**
     * 获取表计当前有效电价。
     * <p>优先从关联的费率策略获取固定电价，其次使用表计的 plusPrice。</p>
     */
    private BigDecimal getEffectivePrice(Meter meter) {
        if (meter.getRateConfigId() != null) {
            PriceStrategy strategy = priceStrategyMapper.selectById(meter.getRateConfigId());
            if (strategy != null && strategy.getFixedPrice() != null) {
                BigDecimal fixedPrice = strategy.getFixedPrice();
                if (strategy.getPlusPrice() != null) {
                    fixedPrice = fixedPrice.add(strategy.getPlusPrice());
                }
                return fixedPrice;
            }
        }
        // 回退到表计自身电价
        return meter.getPlusPrice() != null ? meter.getPlusPrice() : BigDecimal.ZERO;
    }

    private String getPriceStrategyName(Long rateConfigId) {
        if (rateConfigId == null) return "未配置";
        PriceStrategy strategy = priceStrategyMapper.selectById(rateConfigId);
        return strategy != null ? strategy.getStrategyName() : "未找到策略";
    }

    private String getMeterTypeName(Integer meterType) {
        if (meterType == null) return "未知";
        MeterType type = MeterType.fromCode(meterType);
        return type != null ? type.getLabel() : "未知类型";
    }

    private String getMeterStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "运行";
            case 2 -> "欠费停机";
            case 3 -> "拆表销户";
            default -> "其他(" + status + ")";
        };
    }

    private LocalDateTime parseDateTime(String str) {
        try {
            return LocalDateTime.parse(str, DTF);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(str + " 00:00:00", DTF);
            } catch (Exception ex) {
                log.warn("时间解析失败: {}", str);
                return LocalDateTime.now();
            }
        }
    }

    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }
}
