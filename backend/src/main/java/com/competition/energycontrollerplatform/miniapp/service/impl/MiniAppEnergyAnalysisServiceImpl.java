package com.competition.energycontrollerplatform.miniapp.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.miniapp.dto.DailyTrendVO;
import com.competition.energycontrollerplatform.miniapp.dto.MonthlyTrendVO;
import com.competition.energycontrollerplatform.miniapp.mapper.MiniAppEnergyAnalysisMapper;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppEnergyAnalysisService;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppMeterAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序用能分析服务实现。
 * <p>
 * 主数据源为预聚合表（tb_meter_daily_report / tb_meter_monthly_report），
 * 无数据时自动降级查询原始读数表（t_meter_reading）。
 * 对缺失日期/月份自动填充 BigDecimal.ZERO，确保前端图表数据连续。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniAppEnergyAnalysisServiceImpl implements MiniAppEnergyAnalysisService {

    private final MiniAppEnergyAnalysisMapper energyAnalysisMapper;
    private final MiniAppMeterAccessService meterAccessService;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // ──────────────── 日用电趋势 ────────────────

    @Override
    public List<DailyTrendVO> getDailyElectricTrend(Long userId, Long customerId, Long meterId, String month) {
        meterAccessService.requireViewable(userId, customerId, meterId);
        if (meterId == null && customerId == null) {
            throw new BusinessException(403, "请先选择已授权的虚拟表");
        }
        YearMonth ym = parseMonth(month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        // 1. 主查询：预聚合日报表
        List<Map<String, Object>> rows = meterId != null
                ? energyAnalysisMapper.sumDailyElectricByMeter(meterId, monthStart, monthEnd)
                : energyAnalysisMapper.sumDailyElectricByMonth(customerId, monthStart, monthEnd);

        // 2. 降级：预聚合表无数据时查原始读数
        if (rows.isEmpty()) {
            log.info("[用能分析] 日报表无数据，降级查询 t_meter_reading。customerId={}, month={}", customerId, month);
            rows = meterId != null
                    ? energyAnalysisMapper.sumDailyElectricReadingByMeter(meterId, monthStart, monthEnd.plusDays(1))
                    : energyAnalysisMapper.sumDailyElectricFromReadings(customerId, monthStart, monthEnd.plusDays(1));
        }

        // 3. 构建 date → usage 映射
        Map<LocalDate, BigDecimal> usageMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = toLocalDate(row.get("statDate"));
            BigDecimal usage = toBigDecimal(row.get("dailyUsage"));
            if (date != null) {
                usageMap.put(date, usage);
            }
        }

        // 4. 填充缺失日期为 BigDecimal.ZERO
        return fillDailyGaps(usageMap, monthStart, monthEnd);
    }

    // ──────────────── 月用电趋势 ────────────────

    @Override
    public List<MonthlyTrendVO> getMonthlyElectricTrend(Long userId, Long customerId, Long meterId, String year) {
        meterAccessService.requireViewable(userId, customerId, meterId);
        if (meterId == null && customerId == null) {
            throw new BusinessException(403, "请先选择已授权的虚拟表");
        }
        int yearInt = parseYear(year);

        // 1. 主查询：预聚合月报表
        List<Map<String, Object>> rows = meterId != null
                ? energyAnalysisMapper.sumMonthlyElectricByMeter(meterId, year)
                : energyAnalysisMapper.sumMonthlyElectricByYear(customerId, year);

        // 2. 降级：预聚合表无数据时查原始读数
        if (rows.isEmpty()) {
            log.info("[用能分析] 月报表无数据，降级查询 t_meter_reading。customerId={}, year={}", customerId, year);
            rows = meterId != null
                    ? energyAnalysisMapper.sumMonthlyElectricReadingByMeter(meterId, yearInt)
                    : energyAnalysisMapper.sumMonthlyElectricFromReadings(customerId, yearInt);
        }

        // 3. 构建 monthStr → usage 映射
        Map<String, BigDecimal> usageMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String monthStr = toString(row.get("statMonth"));
            BigDecimal usage = toBigDecimal(row.get("monthlyUsage"));
            if (monthStr != null && !monthStr.isEmpty()) {
                usageMap.put(monthStr, usage);
            }
        }

        // 4. 填充缺失月份为 BigDecimal.ZERO
        return fillMonthlyGaps(usageMap, yearInt);
    }

    // ──────────────── 日期填充 ────────────────

    /**
     * 将不连续的日数据填充为完整的月度序列（缺失日期填0）。
     */
    private List<DailyTrendVO> fillDailyGaps(Map<LocalDate, BigDecimal> usageMap,
                                              LocalDate start, LocalDate end) {
        List<DailyTrendVO> trends = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            trends.add(DailyTrendVO.builder()
                    .date(cursor.toString())
                    .usage(usageMap.getOrDefault(cursor, BigDecimal.ZERO))
                    .build());
            cursor = cursor.plusDays(1);
        }
        return trends;
    }

    /**
     * 将不连续的月数据填充为完整的年度序列（缺失月份填0）。
     */
    private List<MonthlyTrendVO> fillMonthlyGaps(Map<String, BigDecimal> usageMap, int year) {
        List<MonthlyTrendVO> trends = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String monthStr = String.format("%d-%02d", year, m);
            trends.add(MonthlyTrendVO.builder()
                    .month(monthStr)
                    .usage(usageMap.getOrDefault(monthStr, BigDecimal.ZERO))
                    .build());
        }
        return trends;
    }

    // ──────────────── 参数校验 ────────────────

    /**
     * 解析并校验月份参数。
     */
    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException("月份格式错误，应为 yyyy-MM，实际值: " + month);
        }
    }

    /**
     * 解析并校验年份参数。
     */
    private int parseYear(String year) {
        if (year == null || year.isBlank()) {
            return LocalDate.now().getYear();
        }
        if (!year.matches("\\d{4}")) {
            throw new BusinessException("年份格式错误，应为 yyyy，实际值: " + year);
        }
        return Integer.parseInt(year);
    }

    // ──────────────── 类型转换工具 ────────────────

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date sd) return sd.toLocalDate();
        try {
            return LocalDate.parse(val.toString().substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private String toString(Object val) {
        return val != null ? val.toString() : null;
    }
}
