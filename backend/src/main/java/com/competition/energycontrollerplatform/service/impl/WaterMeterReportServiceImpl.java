package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.mapper.WaterMeterReportMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.WaterMeterReportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 水表用水量报表服务实现。
 * <p>
 * 直接从水表读数流水表（t_water_meter_reading）实时聚合，
 * 不做预聚合表兜底（最基础的实现）。
 * 非 ADMIN 角色强制注入当前商户 ID 实现数据权限隔离。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Service
@RequiredArgsConstructor
public class WaterMeterReportServiceImpl implements WaterMeterReportService {

    private static final Logger log = LoggerFactory.getLogger(WaterMeterReportServiceImpl.class);

    private final WaterMeterReportMapper waterMeterReportMapper;

    // ──────────────────────── 日报表明细 ────────────────────────

    @Override
    public PageResultVO<WaterDailyUsageVO> listDailyDetail(int pageNum, int pageSize,
                                                            LocalDate startDate, LocalDate endDate,
                                                            Long merchantId, String merchantName, String deviceNo) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> allRows = waterMeterReportMapper.selectDailyDetail(
                startDate, endDate, effectiveMerchantId, merchantName, deviceNo);

        // 手动分页（SQL 含动态 WHERE + JOIN，不适合 MyBatis-Plus 自动分页）
        int total = allRows.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<WaterDailyUsageVO> pageRows = new ArrayList<>();
        if (fromIndex < total) {
            for (Map<String, Object> row : allRows.subList(fromIndex, toIndex)) {
                pageRows.add(mapToDailyVO(row));
            }
        }

        log.info("[水表日报明细] page={}/{}, total={}, effectiveMerchantId={}",
                pageNum, pageSize, total, effectiveMerchantId);
        return PageResultVO.of(pageRows, total);
    }

    // ──────────────────────── 日报汇总 ────────────────────────

    @Override
    public List<WaterDailyUsageVO> sumDaily(LocalDate startDate, LocalDate endDate,
                                             Long merchantId, String deviceNo) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> rows = waterMeterReportMapper.sumDaily(
                startDate, endDate, effectiveMerchantId, deviceNo);

        List<WaterDailyUsageVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            WaterDailyUsageVO vo = new WaterDailyUsageVO();
            vo.setStatDate(convertToLocalDate(row.get("statDate")));
            vo.setTotalUsage(convertToBigDecimal(row.get("totalUsage")));
            vo.setTotalFee(convertToBigDecimal(row.get("totalFee")));
            vo.setMeterCount(convertToInteger(row.get("meterCount")));
            result.add(vo);
        }

        log.info("[水表日报汇总] rows={}, effectiveMerchantId={}", result.size(), effectiveMerchantId);
        return result;
    }

    // ──────────────────────── 月报表明细 ────────────────────────

    @Override
    public PageResultVO<WaterMonthlyUsageVO> listMonthlyDetail(int pageNum, int pageSize,
                                                                String statMonth, String statYear,
                                                                Long merchantId, String merchantName, String deviceNo) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> allRows = waterMeterReportMapper.selectMonthlyDetail(
                statMonth, statYear, effectiveMerchantId, merchantName, deviceNo);

        int total = allRows.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<WaterMonthlyUsageVO> pageRows = new ArrayList<>();
        if (fromIndex < total) {
            for (Map<String, Object> row : allRows.subList(fromIndex, toIndex)) {
                pageRows.add(mapToMonthlyVO(row));
            }
        }

        log.info("[水表月报明细] page={}/{}, total={}, statMonth={}, statYear={}",
                pageNum, pageSize, total, statMonth, statYear);
        return PageResultVO.of(pageRows, total);
    }

    // ──────────────────────── 月报汇总 ────────────────────────

    @Override
    public List<WaterMonthlyUsageVO> sumMonthly(String statMonth, Long merchantId, String deviceNo) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> rows = waterMeterReportMapper.sumMonthly(
                statMonth, effectiveMerchantId, deviceNo);

        List<WaterMonthlyUsageVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            WaterMonthlyUsageVO vo = new WaterMonthlyUsageVO();
            vo.setStatMonth((String) row.get("statMonth"));
            vo.setTotalUsage(convertToBigDecimal(row.get("totalUsage")));
            vo.setTotalFee(convertToBigDecimal(row.get("totalFee")));
            vo.setDailyCount(convertToInteger(row.get("dailyCount")));
            vo.setMeterCount(convertToInteger(row.get("meterCount")));
            result.add(vo);
        }

        log.info("[水表月报汇总] rows={}, statMonth={}", result.size(), statMonth);
        return result;
    }

    // ──────────────────────── 已删除水表月报表 ────────────────────────

    @Override
    public List<WaterDeletedMeterMonthlyVO> getDeletedMeterMonthlyReport(String statMonth,
                                                                          Long merchantId,
                                                                          String merchantName,
                                                                          String deviceNo) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> rows = waterMeterReportMapper.selectDeletedMeterMonthly(
                statMonth, effectiveMerchantId, merchantName, deviceNo);

        List<WaterDeletedMeterMonthlyVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(mapToDeletedMeterVO(row));
        }

        log.info("[已删除水表月报] rows={}, statMonth={}, effectiveMerchantId={}",
                result.size(), statMonth, effectiveMerchantId);
        return result;
    }

    // ──────────────────────── 全园区月度汇总（管理层视角） ────────────────────────

    @Override
    public List<WaterMonthlySummaryVO> getCampusMonthlySummary(String statYear,
                                                                LocalDate startDate, LocalDate endDate,
                                                                Long merchantId) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> rows = waterMeterReportMapper.sumCampusMonthly(
                statYear, startDate, endDate, effectiveMerchantId);

        List<WaterMonthlySummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            WaterMonthlySummaryVO vo = new WaterMonthlySummaryVO();
            vo.setStatMonth((String) row.get("statMonth"));
            vo.setTotalUsage(convertToBigDecimal(row.get("totalUsage")));
            vo.setTotalFee(convertToBigDecimal(row.get("totalFee")));
            vo.setMeterCount(convertToInteger(row.get("meterCount")));
            vo.setDailyCount(convertToInteger(row.get("dailyCount")));
            result.add(vo);
        }

        log.info("[园区月度汇总] rows={}, statYear={}, effectiveMerchantId={}",
                result.size(), statYear, effectiveMerchantId);
        return result;
    }

    // ──────────────────────── 全园区年度汇总（管理层视角） ────────────────────────

    @Override
    public List<WaterYearlySummaryVO> getCampusYearlySummary(LocalDate startDate, LocalDate endDate,
                                                              Long merchantId) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> rows = waterMeterReportMapper.sumCampusYearly(
                startDate, endDate, effectiveMerchantId);

        List<WaterYearlySummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            WaterYearlySummaryVO vo = new WaterYearlySummaryVO();
            vo.setStatYear((String) row.get("statYear"));
            vo.setTotalUsage(convertToBigDecimal(row.get("totalUsage")));
            vo.setTotalFee(convertToBigDecimal(row.get("totalFee")));
            vo.setMeterCount(convertToInteger(row.get("meterCount")));
            vo.setMonthCount(convertToInteger(row.get("monthCount")));
            result.add(vo);
        }

        log.info("[园区年度汇总] rows={}, effectiveMerchantId={}",
                result.size(), effectiveMerchantId);
        return result;
    }

    // ──────────────────────── 计费水量汇总表（财务视角，按天聚合） ────────────────────────

    @Override
    public List<WaterBillingSummaryVO> getBillingDailySummary(LocalDate startDate, LocalDate endDate,
                                                               Long merchantId) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> rows = waterMeterReportMapper.sumBillingDaily(
                startDate, endDate, effectiveMerchantId);

        List<WaterBillingSummaryVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            WaterBillingSummaryVO vo = new WaterBillingSummaryVO();
            vo.setStatDate(toStringValue(row.get("statDate")));
            BigDecimal totalUsage = convertToBigDecimal(row.get("totalUsage"));
            BigDecimal totalFee = convertToBigDecimal(row.get("totalFee"));
            vo.setTotalUsage(totalUsage);
            vo.setTotalFee(totalFee);
            vo.setAveragePrice(calcAveragePrice(totalFee, totalUsage));
            vo.setMeterCount(convertToInteger(row.get("meterCount")));
            result.add(vo);
        }

        log.info("[计费水量日汇总] rows={}, effectiveMerchantId={}", result.size(), effectiveMerchantId);
        return result;
    }

    // ──────────────────────── 计费水量月报表（财务视角，按月聚合） ────────────────────────

    @Override
    public List<WaterBillingMonthlyVO> getBillingMonthlySummary(String statMonth, String statYear,
                                                                 Long merchantId) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> rows = waterMeterReportMapper.sumBillingMonthly(
                statMonth, statYear, effectiveMerchantId);

        List<WaterBillingMonthlyVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            WaterBillingMonthlyVO vo = new WaterBillingMonthlyVO();
            vo.setStatMonth((String) row.get("statMonth"));
            BigDecimal totalUsage = convertToBigDecimal(row.get("totalUsage"));
            BigDecimal totalFee = convertToBigDecimal(row.get("totalFee"));
            vo.setTotalUsage(totalUsage);
            vo.setTotalFee(totalFee);
            vo.setAveragePrice(calcAveragePrice(totalFee, totalUsage));
            vo.setMeterCount(convertToInteger(row.get("meterCount")));
            vo.setDailyCount(convertToInteger(row.get("dailyCount")));
            result.add(vo);
        }

        log.info("[计费水量月报表] rows={}, statMonth={}, statYear={}", result.size(), statMonth, statYear);
        return result;
    }

    // ──────────────────────── 计费水量年报表（财务视角，按年聚合） ────────────────────────

    @Override
    public List<WaterBillingYearlyVO> getBillingYearlySummary(LocalDate startDate, LocalDate endDate,
                                                               Long merchantId) {
        Long effectiveMerchantId = resolveMerchantId(merchantId);

        List<Map<String, Object>> rows = waterMeterReportMapper.sumBillingYearly(
                startDate, endDate, effectiveMerchantId);

        List<WaterBillingYearlyVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            WaterBillingYearlyVO vo = new WaterBillingYearlyVO();
            vo.setStatYear((String) row.get("statYear"));
            BigDecimal totalUsage = convertToBigDecimal(row.get("totalUsage"));
            BigDecimal totalFee = convertToBigDecimal(row.get("totalFee"));
            vo.setTotalUsage(totalUsage);
            vo.setTotalFee(totalFee);
            vo.setAveragePrice(calcAveragePrice(totalFee, totalUsage));
            vo.setMeterCount(convertToInteger(row.get("meterCount")));
            vo.setMonthCount(convertToInteger(row.get("monthCount")));
            result.add(vo);
        }

        log.info("[计费水量年报表] rows={}, effectiveMerchantId={}", result.size(), effectiveMerchantId);
        return result;
    }

    // ──────────────────────── 权限隔离 ────────────────────────

    /**
     * 解析有效的商户 ID。
     * <p>非 ADMIN 用户强制覆盖为其自身的商户 ID，实现数据权限隔离。</p>
     * <p>非 ADMIN 且未关联商户时抛出 403，fail-fast 防止越权。</p>
     *
     * @throws BusinessException 非 ADMIN 且未关联商户时抛出 403
     */
    private Long resolveMerchantId(Long requestedMerchantId) {
        if (SecurityUserContext.isAdmin()) {
            return requestedMerchantId;
        }
        Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
        if (myCustomerId == null) {
            log.warn("[权限拒绝] 非 ADMIN 且未关联商户的用户尝试访问水表报表");
            throw new BusinessException(403, "无权限：未关联商户");
        }
        return myCustomerId;
    }

    // ──────────────────────── VO 映射 ────────────────────────

    /**
     * Map → WaterDailyUsageVO 映射。
     */
    private WaterDailyUsageVO mapToDailyVO(Map<String, Object> row) {
        WaterDailyUsageVO vo = new WaterDailyUsageVO();
        vo.setStatDate(convertToLocalDate(row.get("statDate")));
        vo.setMeterId(toLongValue(row.get("meterId")));
        vo.setMerchantId(toLongValue(row.get("merchantId")));
        vo.setMerchantName((String) row.get("merchantName"));
        vo.setDeviceNo((String) row.get("deviceNo"));
        vo.setVirtualMeterNo((String) row.get("virtualMeterNo"));
        vo.setVirtualMerchantName((String) row.get("virtualMerchantName"));
        vo.setTotalUsage(convertToBigDecimal(row.get("totalUsage")));
        vo.setTotalFee(convertToBigDecimal(row.get("totalFee")));
        return vo;
    }

    /**
     * Map → WaterMonthlyUsageVO 映射。
     */
    private WaterMonthlyUsageVO mapToMonthlyVO(Map<String, Object> row) {
        WaterMonthlyUsageVO vo = new WaterMonthlyUsageVO();
        vo.setStatMonth((String) row.get("statMonth"));
        vo.setMeterId(toLongValue(row.get("meterId")));
        vo.setMerchantId(toLongValue(row.get("merchantId")));
        vo.setMerchantName((String) row.get("merchantName"));
        vo.setDeviceNo((String) row.get("deviceNo"));
        vo.setVirtualMeterNo((String) row.get("virtualMeterNo"));
        vo.setVirtualMerchantName((String) row.get("virtualMerchantName"));
        vo.setTotalUsage(convertToBigDecimal(row.get("totalUsage")));
        vo.setTotalFee(convertToBigDecimal(row.get("totalFee")));
        vo.setDailyCount(convertToInteger(row.get("dailyCount")));
        return vo;
    }

    /**
     * Map → WaterDeletedMeterMonthlyVO 映射。
     */
    private WaterDeletedMeterMonthlyVO mapToDeletedMeterVO(Map<String, Object> row) {
        WaterDeletedMeterMonthlyVO vo = new WaterDeletedMeterMonthlyVO();
        vo.setStatMonth((String) row.get("statMonth"));
        vo.setMeterId(toLongValue(row.get("meterId")));
        vo.setMerchantId(toLongValue(row.get("merchantId")));
        vo.setMerchantName((String) row.get("merchantName"));
        vo.setDeviceNo((String) row.get("deviceNo"));
        vo.setTotalUsage(convertToBigDecimal(row.get("totalUsage")));
        vo.setTotalFee(convertToBigDecimal(row.get("totalFee")));
        vo.setDailyCount(convertToInteger(row.get("dailyCount")));
        vo.setDeletedTime(convertToLocalDate(row.get("deletedTime")));
        vo.setDataSource((String) row.get("dataSource"));
        return vo;
    }

    // ──────────────────────── 财务核费工具 ────────────────────────

    /**
     * 计算平均单价 = totalFee ÷ totalUsage。
     * <p>保留 4 位小数，避免除零异常。</p>
     */
    private BigDecimal calcAveragePrice(BigDecimal totalFee, BigDecimal totalUsage) {
        if (totalUsage == null || totalUsage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalFee.divide(totalUsage, 4, RoundingMode.HALF_UP);
    }

    // ──────────────────────── 类型转换工具 ────────────────────────

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

    private Integer convertToInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer i) return i;
        if (value instanceof Long l) return l.intValue();
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Long toLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate convertToLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date sd) return sd.toLocalDate();
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Object → String 安全转换。
     * <p>MySQL JDBC 的 DATE() 函数可能返回 java.sql.Date，需特殊处理。</p>
     */
    private String toStringValue(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof java.sql.Date sd) return sd.toLocalDate().toString();
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate().toString();
        return value.toString();
    }
}
