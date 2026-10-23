package com.competition.energycontrollerplatform.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.entity.*;
import com.competition.energycontrollerplatform.mapper.*;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.PublicAllocationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 公共分摊服务实现。
 * <p>
 * <b>核心安全原则：</b>
 * <ul>
 *   <li>所有金额计算使用 BigDecimal，杜绝 float/double</li>
 *   <li>余额扣减使用 SQL 原子更新 {@code SET balance = balance - #{amount} AND balance >= #{amount}}</li>
 *   <li>仅 ADMIN 可执行分摊操作</li>
 *   <li>整个分摊过程在 {@code @Transactional} 事务内，任意一步失败即全部回滚</li>
 *   <li>每台表计扣减成功后写入 t_account_flow（trade_type=7 公共分摊）</li>
 * </ul>
 *
 * <b>分摊策略：</b>
 * <ul>
 *   <li>{@code PER_HOUSEHOLD} — 按户等分：总金额 / 目标表计数</li>
 *   <li>{@code BY_USAGE_RATIO} — 按近30天历史用量比例分摊</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PublicAllocationServiceImpl implements PublicAllocationService {

    private static final Logger log = LoggerFactory.getLogger(PublicAllocationServiceImpl.class);

    private final PublicAllocationMapper publicAllocationMapper;
    private final AccountFlowMapper accountFlowMapper;
    private final MeterMapper meterMapper;
    private final CustomerMapper customerMapper;
    private final ReportDailyElectricityMapper reportDailyElectricityMapper;

    // ──────────────────────── 执行分摊 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicAllocationResultVO execute(PublicAllocationExecuteRequest request) {
        // 仅 ADMIN 可执行分摊
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行公共分摊操作");
        }

        // 解析总表表号 → sourceMeterId
        resolveSourceMeterNo(request);

        String currentUser = SecurityUserContext.getCurrentUsername();
        BigDecimal totalAmount = request.getTotalAmount();
        List<Long> targetMeterIds = request.getTargetMeterIds();

        log.info("[公共分摊] 开始执行, totalAmount={}, strategy={}, meterCount={}, operator={}",
                totalAmount, request.getAllocationType(), targetMeterIds.size(), currentUser);

        // ① 校验所有目标表计存在并获取其商户信息
        Map<Long, Meter> meterMap = new LinkedHashMap<>();
        for (Long meterId : targetMeterIds) {
            Meter meter = meterMapper.selectById(meterId);
            if (meter == null) {
                throw BusinessException.of(400, "表计不存在: id=" + meterId);
            }
            if (meter.getCustomerId() == null) {
                throw BusinessException.of(400, "表计未绑定商户，无法分摊: id=" + meterId);
            }
            meterMap.put(meterId, meter);
        }

        // ② 计算每台表计的分摊金额
        Map<Long, BigDecimal> allocationMap;
        if ("BY_USAGE_RATIO".equals(request.getAllocationType())) {
            allocationMap = calculateByUsageRatio(meterMap, totalAmount);
        } else {
            // 默认 PER_HOUSEHOLD — 按户等分
            allocationMap = calculateByHousehold(meterMap, totalAmount);
        }

        // ③ 执行逐表原子扣减 + 写入流水
        List<PublicAllocationResultVO.AllocationItem> items = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Long, Meter> entry : meterMap.entrySet()) {
            Long meterId = entry.getKey();
            Meter meter = entry.getValue();
            BigDecimal allocatedAmount = allocationMap.get(meterId);
            Long customerId = meter.getCustomerId();

            PublicAllocationResultVO.AllocationItem item = new PublicAllocationResultVO.AllocationItem();
            item.setMeterId(meterId);
            item.setMeterNo(meter.getStealNo() != null ? meter.getStealNo() : meter.getDbNo());
            item.setCustomerId(customerId);
            item.setAllocatedAmount(allocatedAmount);

            // 计算比例
            BigDecimal ratio = BigDecimal.ZERO;
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                ratio = allocatedAmount.divide(totalAmount, 4, RoundingMode.HALF_UP);
            }
            item.setRatio(ratio);

            // 查询扣减前余额
            Customer customer = customerMapper.selectById(customerId);
            if (customer == null) {
                item.setSuccess(false);
                item.setFailReason("商户不存在");
                failCount++;
                items.add(item);
                continue;
            }
            BigDecimal balanceBefore = customer.getBalance();
            item.setBalanceBefore(balanceBefore);

            // 原子扣减余额
            int rows = customerMapper.subtractBalance(customerId, allocatedAmount);
            if (rows == 0) {
                // 余额不足
                BigDecimal currentBalance = customerMapper.selectById(customerId).getBalance();
                item.setSuccess(false);
                item.setBalanceAfter(currentBalance);
                item.setFailReason("余额不足：当前余额 " + currentBalance + " < 分摊金额 " + allocatedAmount);
                failCount++;
                items.add(item);
                log.warn("[公共分摊] 余额不足 meterId={}, customerId={}, balance={}, allocated={}",
                        meterId, customerId, currentBalance, allocatedAmount);
                continue;
            }

            // 查询扣减后余额
            Customer afterCustomer = customerMapper.selectById(customerId);
            BigDecimal balanceAfter = afterCustomer.getBalance();
            item.setBalanceAfter(balanceAfter);
            item.setSuccess(true);

            // 写入资金流水（trade_type=7 公共分摊）
            String flowNo = generateFlowNo("PUB");
            AccountFlow flow = new AccountFlow();
            flow.setFlowNo(flowNo);
            flow.setCustomerId(customerId);
            flow.setMeterId(meterId);
            flow.setBalanceBefore(balanceBefore);
            flow.setChangeAmount(allocatedAmount.negate()); // 出账=负数
            flow.setBalanceAfter(balanceAfter);
            flow.setTradeType(7); // 公共分摊
            flow.setBizNo(flowNo);
            flow.setOperatorName(currentUser);
            flow.setOperateTime(LocalDateTime.now());
            flow.setRemark("公共分摊 - " + (request.getAllocationName() != null ? request.getAllocationName() : "")
                    + (request.getRemark() != null ? " - " + request.getRemark() : ""));
            flow.setCreateTime(LocalDateTime.now());
            flow.setUpdateTime(LocalDateTime.now());
            accountFlowMapper.insert(flow);

            // 填充客户名称
            item.setCustomerName(afterCustomer.getCustomerName());

            successCount++;
            items.add(item);
        }

        // ④ 写入 t_public_allocation 分摊记录
        PublicAllocation allocation = new PublicAllocation();
        allocation.setProjectId(request.getProjectId());
        allocation.setSourceMeterId(request.getSourceMeterId());
        allocation.setAllocationName(request.getAllocationName());
        allocation.setAllocationType(request.getAllocationType());
        allocation.setTotalAmount(totalAmount);
        allocation.setAllocationDetail(buildAllocationDetailJson(items));
        allocation.setEffectiveDate(request.getEffectiveDate() != null
                ? LocalDate.parse(request.getEffectiveDate())
                : LocalDate.now());
        allocation.setStatus(1);
        allocation.setRemark(request.getRemark());
        allocation.setCreateBy(currentUser);
        allocation.setCreateTime(LocalDateTime.now());
        allocation.setUpdateTime(LocalDateTime.now());
        publicAllocationMapper.insert(allocation);

        // ⑤ 组装返回结果
        PublicAllocationResultVO result = new PublicAllocationResultVO();
        result.setAllocationId(allocation.getId());
        result.setAllocationName(request.getAllocationName());
        result.setTotalAmount(totalAmount);
        result.setAllocationType(request.getAllocationType());
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setItems(items);

        log.info("[公共分摊] 完成 allocationId={}, success={}, fail={}, totalAmount={}",
                allocation.getId(), successCount, failCount, totalAmount);

        return result;
    }

    /**
     * 根据总表表号自动查询并设置 sourceMeterId。
     * <p>若请求中传了 sourceMeterNo，优先通过表号查库获取 ID；
     * 查不到则抛出清晰错误提示。若 sourceMeterNo 为空且 sourceMeterId 也为空则报错。</p>
     */
    private void resolveSourceMeterNo(PublicAllocationExecuteRequest request) {
        if (StringUtils.hasText(request.getSourceMeterNo())) {
            // 按电表表号（dbNo）精确匹配
            LambdaQueryWrapper<Meter> wrapper = new LambdaQueryWrapper<Meter>()
                    .eq(Meter::getDbNo, request.getSourceMeterNo().trim())
                    .eq(Meter::getIsDeleted, 0);
            Meter meter = meterMapper.selectOne(wrapper);
            if (meter == null) {
                throw BusinessException.of(400,
                        "总表表号「" + request.getSourceMeterNo().trim() + "」不存在，请检查表号是否正确");
            }
            request.setSourceMeterId(meter.getId());
        }
        if (request.getSourceMeterId() == null) {
            throw BusinessException.of(400, "总表ID和总表表号至少填写一项");
        }
    }

    // ──────────────────────── 列表查询 ────────────────────────

    @Override
    public IPage<PublicAllocationResultVO> listPage(PublicAllocationPageRequest request) {
        // 权限隔离：仅 ADMIN 可查看全部分摊记录
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可查看公共分摊记录");
        }

        LambdaQueryWrapper<PublicAllocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getAllocationName()),
                        PublicAllocation::getAllocationName, request.getAllocationName())
               .eq(StringUtils.hasText(request.getAllocationType()),
                        PublicAllocation::getAllocationType, request.getAllocationType())
               .eq(request.getStatus() != null,
                        PublicAllocation::getStatus, request.getStatus());

        // 日期范围筛选
        if (StringUtils.hasText(request.getStartDate())) {
            wrapper.ge(PublicAllocation::getEffectiveDate, LocalDate.parse(request.getStartDate()));
        }
        if (StringUtils.hasText(request.getEndDate())) {
            wrapper.le(PublicAllocation::getEffectiveDate, LocalDate.parse(request.getEndDate()));
        }

        wrapper.orderByDesc(PublicAllocation::getCreateTime);

        Page<PublicAllocation> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<PublicAllocation> result = publicAllocationMapper.selectPage(page, wrapper);
        return result.convert(this::toResultVO);
    }

    @Override
    public List<PublicAllocationExcelVO> exportList(PublicAllocationPageRequest request, List<Long> ids) {
        // 权限隔离：仅 ADMIN 可导出
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可导出公共分摊记录");
        }

        LambdaQueryWrapper<PublicAllocation> wrapper = new LambdaQueryWrapper<>();

        // 如果指定了 ID 列表，优先使用
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(PublicAllocation::getId, ids);
        }

        // 附加筛选条件
        if (request != null) {
            wrapper.like(StringUtils.hasText(request.getAllocationName()),
                            PublicAllocation::getAllocationName, request.getAllocationName())
                   .eq(StringUtils.hasText(request.getAllocationType()),
                            PublicAllocation::getAllocationType, request.getAllocationType())
                   .eq(request.getStatus() != null,
                            PublicAllocation::getStatus, request.getStatus());

            if (StringUtils.hasText(request.getStartDate())) {
                wrapper.ge(PublicAllocation::getEffectiveDate, LocalDate.parse(request.getStartDate()));
            }
            if (StringUtils.hasText(request.getEndDate())) {
                wrapper.le(PublicAllocation::getEffectiveDate, LocalDate.parse(request.getEndDate()));
            }
        }

        wrapper.orderByDesc(PublicAllocation::getCreateTime);

        List<PublicAllocation> list = publicAllocationMapper.selectList(wrapper);
        return list.stream().map(this::toExcelVO).collect(Collectors.toList());
    }

    // ──────────────────────── 分摊统计 ────────────────────────

    @Override
    public List<PublicAllocationSummaryVO> getAllocationSummary(PublicAllocationSummaryRequest request) {
        // 权限隔离：仅 ADMIN 可查看
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可查看公共分摊统计");
        }

        String granularity = request.getGranularity() != null ? request.getGranularity().toUpperCase() : "MONTH";
        List<Map<String, Object>> rows;

        // 直接在 MySQL 层 SUM + GROUP BY，绝不捞回 JVM
        switch (granularity) {
            case "DAY":
                rows = publicAllocationMapper.sumByDay(request.getStartDate(), request.getEndDate(), request.getAllocationType());
                break;
            case "YEAR":
                rows = publicAllocationMapper.sumByYear(request.getStartDate(), request.getEndDate(), request.getAllocationType());
                break;
            case "MONTH":
            default:
                rows = publicAllocationMapper.sumByMonth(request.getStartDate(), request.getEndDate(), request.getAllocationType());
                break;
        }

        List<PublicAllocationSummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            PublicAllocationSummaryVO vo = new PublicAllocationSummaryVO();
            vo.setPeriod(toStringValue(row.get("period")));
            BigDecimal totalAmount = convertToBigDecimal(row.get("totalAmount"));
            Long allocationCount = toLongValue(row.get("allocationCount"));
            vo.setTotalAllocatedAmount(totalAmount);
            vo.setAllocationCount(allocationCount);
            // 平均单次分摊金额 = 总金额 / 分摊次数
            if (allocationCount != null && allocationCount > 0) {
                vo.setAvgAmount(totalAmount.divide(BigDecimal.valueOf(allocationCount), 2, RoundingMode.HALF_UP));
            } else {
                vo.setAvgAmount(BigDecimal.ZERO);
            }
            result.add(vo);
        }

        log.info("[分摊统计] granularity={}, allocationType={}, rows={}",
                granularity, request.getAllocationType(), result.size());
        return result;
    }

    @Override
    public void exportAllocationSummary(HttpServletResponse response, PublicAllocationSummaryRequest request) {
        // 权限隔离
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可导出公共分摊统计");
        }

        // 1. 复用已有统计聚合方法
        List<PublicAllocationSummaryVO> summaryList = getAllocationSummary(request);

        // 2. 设置 HTTP 响应头
        String granularity = request.getGranularity() != null ? request.getGranularity().toUpperCase() : "MONTH";
        String fileName = "分账统计_" + granularity + ".xlsx";
        setExcelResponse(response, fileName);

        // 3. 流式写入 Excel（EasyExcel 内部流式写入磁盘，不会 OOM）
        try {
            EasyExcel.write(response.getOutputStream(), PublicAllocationSummaryVO.class)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet("分账统计")
                    .doWrite(summaryList);
        } catch (IOException e) {
            log.error("[Excel导出] 分账统计导出失败", e);
            throw new RuntimeException("Excel 导出失败", e);
        }

        log.info("[Excel导出] 分账统计导出完成，granularity={}, 共 {} 行", granularity, summaryList.size());
    }

    // ──────────────────────── 分摊计算策略 ────────────────────────

    /**
     * 按户等分：总金额 / 表计数。
     * <p>尾差处理：最后一家承担所有舍入误差，确保总和 = totalAmount。</p>
     */
    private Map<Long, BigDecimal> calculateByHousehold(Map<Long, Meter> meterMap, BigDecimal totalAmount) {
        int count = meterMap.size();
        BigDecimal perMeter = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal allocated = BigDecimal.ZERO;
        List<Long> ids = new ArrayList<>(meterMap.keySet());

        for (int i = 0; i < ids.size(); i++) {
            if (i == ids.size() - 1) {
                // 最后一家承担尾差
                BigDecimal lastAmount = totalAmount.subtract(allocated);
                result.put(ids.get(i), lastAmount.max(BigDecimal.ZERO));
            } else {
                result.put(ids.get(i), perMeter);
                allocated = allocated.add(perMeter);
            }
        }
        return result;
    }

    /**
     * 按历史用量比例分摊（近30天）。
     * <p>各表分摊金额 = (该表近30天用电量 / 总用电量) × totalAmount。</p>
     * <p>如果所有表近期均无用量的极端情况，回退到按户等分。</p>
     */
    private Map<Long, BigDecimal> calculateByUsageRatio(Map<Long, Meter> meterMap, BigDecimal totalAmount) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        List<Long> meterIds = new ArrayList<>(meterMap.keySet());
        List<Map<String, Object>> rows = reportDailyElectricityMapper.sumByMeterIds(
                meterIds, thirtyDaysAgo, today);

        // 构建 meterId → totalConsumed 映射
        Map<Long, BigDecimal> usageMap = new LinkedHashMap<>();
        BigDecimal totalUsage = BigDecimal.ZERO;

        for (Long meterId : meterIds) {
            usageMap.put(meterId, BigDecimal.ZERO);
        }
        for (Map<String, Object> row : rows) {
            Long meterId = (Long) row.get("meterId");
            BigDecimal consumed = convertToBigDecimal(row.get("totalConsumed"));
            usageMap.put(meterId, consumed);
            totalUsage = totalUsage.add(consumed);
        }

        // 如果所有表近期均无用量，回退到按户等分
        if (totalUsage.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[公共分摊] 近30天所有目标表计用电量均为0，回退到按户等分");
            return calculateByHousehold(meterMap, totalAmount);
        }

        // 按比例分配，尾差由最后一家承担
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal allocated = BigDecimal.ZERO;
        List<Long> ids = new ArrayList<>(meterMap.keySet());

        for (int i = 0; i < ids.size(); i++) {
            Long meterId = ids.get(i);
            BigDecimal usage = usageMap.get(meterId);

            if (i == ids.size() - 1) {
                // 最后一家承担尾差
                BigDecimal lastAmount = totalAmount.subtract(allocated);
                result.put(meterId, lastAmount.max(BigDecimal.ZERO));
            } else {
                BigDecimal ratio = usage.divide(totalUsage, 6, RoundingMode.HALF_UP);
                BigDecimal amount = totalAmount.multiply(ratio).setScale(2, RoundingMode.DOWN);
                result.put(meterId, amount);
                allocated = allocated.add(amount);
            }
        }
        return result;
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 生成流水号。
     */
    private String generateFlowNo(String prefix) {
        String ts = String.valueOf(System.currentTimeMillis());
        String shortTs = ts.substring(ts.length() - 8);
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + shortTs + shortUuid;
    }

    /**
     * 将分摊明细构建为 JSON 字符串（存入 t_public_allocation.allocation_detail）。
     */
    private String buildAllocationDetailJson(List<PublicAllocationResultVO.AllocationItem> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            PublicAllocationResultVO.AllocationItem item = items.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"meterId\":").append(item.getMeterId()).append(",")
              .append("\"allocatedAmount\":").append(item.getAllocatedAmount()).append(",")
              .append("\"ratio\":").append(item.getRatio()).append(",")
              .append("\"success\":").append(item.getSuccess()).append(",")
              .append("\"balanceBefore\":").append(item.getBalanceBefore()).append(",")
              .append("\"balanceAfter\":").append(item.getBalanceAfter());
            if (item.getFailReason() != null) {
                sb.append(",\"failReason\":\"").append(item.getFailReason()).append("\"");
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    // ──────────────────────── VO 转换 ────────────────────────

    /**
     * 将 PublicAllocation 实体转换为分页列表 VO。
     */
    private PublicAllocationResultVO toResultVO(PublicAllocation entity) {
        PublicAllocationResultVO vo = new PublicAllocationResultVO();
        vo.setAllocationId(entity.getId());
        vo.setAllocationName(entity.getAllocationName());
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setAllocationType(entity.getAllocationType());
        // 分页列表不返回逐表明细（items），详情时可单独查询
        return vo;
    }

    /**
     * 将 PublicAllocation 实体转换为 Excel 导出 VO。
     */
    private PublicAllocationExcelVO toExcelVO(PublicAllocation entity) {
        PublicAllocationExcelVO vo = new PublicAllocationExcelVO();
        vo.setId(entity.getId());
        vo.setAllocationName(entity.getAllocationName());
        vo.setAllocationType(entity.getAllocationType());
        vo.setAllocationTypeLabel(allocationTypeLabel(entity.getAllocationType()));
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setProjectId(entity.getProjectId());
        vo.setSourceMeterId(entity.getSourceMeterId());
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(statusLabel(entity.getStatus()));
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setCreateBy(entity.getCreateBy());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 分摊方式编码 → 中文标签。
     */
    private String allocationTypeLabel(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "PER_HOUSEHOLD"   -> "按户等分";
            case "BY_USAGE_RATIO"  -> "按用量比例";
            default                -> type;
        };
    }

    /**
     * 状态编码 → 中文标签。
     */
    private String statusLabel(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0  -> "待执行";
            case 1  -> "已执行";
            case 2  -> "已撤销";
            default -> "其他(" + status + ")";
        };
    }

    /**
     * 安全转换为 BigDecimal。
     */
    private BigDecimal convertToBigDecimal(Object value) {
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

    /**
     * 安全转换为 Long。
     */
    private Long toLongValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 安全转换为 String。
     */
    private String toStringValue(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof java.sql.Date sd) return sd.toLocalDate().toString();
        if (value instanceof LocalDate ld) return ld.toString();
        if (value instanceof LocalDateTime ldt) return ldt.toLocalDate().toString();
        return value.toString();
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
