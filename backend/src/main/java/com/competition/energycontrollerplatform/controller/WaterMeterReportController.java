package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.WaterMeterReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 计报表 - 水表统计 Controller。
 * <p>
 * 提供用水量日报表与月报表查询，数据来源于水表读数流水表实时聚合。
 * 这是最基础的 2 个报表，后续可按此模式扩展尖/峰/平/谷分时、年报、导出等 6 个功能。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Tag(name = "计报表 - 水表统计", description = "用水量日报表/月报表查询（水表专用）")
@RestController
@RequestMapping("/api/v1/water-meter-report")
@RequiredArgsConstructor
public class WaterMeterReportController {

    private final WaterMeterReportService waterMeterReportService;

    // ═══════════════════════════════════════════
    // 用水量日报表
    // ═══════════════════════════════════════════

    @Operation(summary = "用水量日报表明细（按水表维度分页）",
            description = "按天+水表维度查询每日用水量与费用明细。"
                    + "支持按日期范围、商户ID、商户名模糊搜索、设备编号筛选。"
                    + "数据来源：t_water_meter_reading 实时聚合。")
    @GetMapping("/daily")
    public Result<PageResultVO<WaterDailyUsageVO>> dailyDetail(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "开始日期（yyyy-MM-dd）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "商户名称（模糊搜索）") @RequestParam(required = false) String merchantName,
            @Parameter(description = "设备编号") @RequestParam(required = false) String deviceNo) {
        merchantId = resolveMerchantIdForReport(merchantId);
        PageResultVO<WaterDailyUsageVO> result = waterMeterReportService.listDailyDetail(
                pageNum, pageSize, startDate, endDate, merchantId, merchantName, deviceNo);
        return Result.success(result);
    }

    @Operation(summary = "用水量日报表汇总（按日聚合）",
            description = "按天汇总总用水量与总费用，不区分设备维度。"
                    + "返回每日的总用水量 + 总费用 + 参与统计的水表数。")
    @GetMapping("/daily-summary")
    public Result<List<WaterDailyUsageVO>> dailySummary(
            @Parameter(description = "开始日期（yyyy-MM-dd）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "设备编号") @RequestParam(required = false) String deviceNo) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<WaterDailyUsageVO> list = waterMeterReportService.sumDaily(
                startDate, endDate, merchantId, deviceNo);
        return Result.success(list);
    }

    // ═══════════════════════════════════════════
    // 用水量月报表
    // ═══════════════════════════════════════════

    @Operation(summary = "用水量月报表明细（按水表维度分页）",
            description = "按自然月+水表维度查询每月用水量与费用明细。"
                    + "支持按月份、年份、商户ID、商户名模糊搜索、设备编号筛选。"
                    + "数据来源：t_water_meter_reading 实时聚合。")
    @GetMapping("/monthly")
    public Result<PageResultVO<WaterMonthlyUsageVO>> monthlyDetail(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "统计月份（YYYY-MM）", example = "2026-06") @RequestParam(required = false) String statMonth,
            @Parameter(description = "统计年份（YYYY，查整年各月数据）", example = "2026") @RequestParam(required = false) String statYear,
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "商户名称（模糊搜索）") @RequestParam(required = false) String merchantName,
            @Parameter(description = "设备编号") @RequestParam(required = false) String deviceNo) {
        merchantId = resolveMerchantIdForReport(merchantId);
        PageResultVO<WaterMonthlyUsageVO> result = waterMeterReportService.listMonthlyDetail(
                pageNum, pageSize, statMonth, statYear, merchantId, merchantName, deviceNo);
        return Result.success(result);
    }

    @Operation(summary = "用水量月报表汇总（按月聚合）",
            description = "按自然月汇总总用水量与总费用，不区分设备维度。"
                    + "返回每月的总用水量 + 总费用 + 参与统计的水表数 + 有效天数。")
    @GetMapping("/monthly-summary")
    public Result<List<WaterMonthlyUsageVO>> monthlySummary(
            @Parameter(description = "统计月份（YYYY-MM）", example = "2026-06") @RequestParam(required = false) String statMonth,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "设备编号") @RequestParam(required = false) String deviceNo) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<WaterMonthlyUsageVO> list = waterMeterReportService.sumMonthly(
                statMonth, merchantId, deviceNo);
        return Result.success(list);
    }

    // ═══════════════════════════════════════════
    // 用水量月报表（已删除水表）
    // ═══════════════════════════════════════════

    @Operation(summary = "用水量月报表（已删除水表）",
            description = "查询已被逻辑删除（is_deleted=1）的水表在历史月份中的用水量汇总。"
                    + "即使水表在系统中已删除，财务仍可查询到其删除当月及之前的用水数据。"
                    + "数据来源：t_water_meter_reading 实时聚合已删除水表的历史读数。"
                    + "支持按月份、商户ID、商户名模糊搜索、设备编号筛选。"
                    + "注意：本接口绕过 MyBatis-Plus @TableLogic 插件，直接在 XML SQL 中查询 is_deleted=1 的水表。")
    @GetMapping("/deleted-meter-monthly")
    public Result<List<WaterDeletedMeterMonthlyVO>> deletedMeterMonthly(
            @Parameter(description = "统计月份（YYYY-MM）", example = "2026-05") @RequestParam(required = false) String statMonth,
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "商户名称（模糊搜索）") @RequestParam(required = false) String merchantName,
            @Parameter(description = "设备编号") @RequestParam(required = false) String deviceNo) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<WaterDeletedMeterMonthlyVO> list = waterMeterReportService.getDeletedMeterMonthlyReport(
                statMonth, merchantId, merchantName, deviceNo);
        return Result.success(list);
    }

    // ═══════════════════════════════════════════
    // 全园区月度/年度用水量汇总（管理层宏观视角）
    // ═══════════════════════════════════════════

    @Operation(summary = "全园区月度用水量汇总（折线图/柱状图）",
            description = "管理层宏观视角：按自然月聚合全园区所有水表的总用水量与总费用。"
                    + "不区分具体水表或商户维度，返回按月升序排列的数据，"
                    + "适配前端 ECharts / Chart.js 等图表库直接渲染折线图或柱状图。"
                    + "X轴=statMonth（YYYY-MM），Y轴=totalUsage（吨）。"
                    + "数据来源：t_water_meter_reading 按 DATE_FORMAT(record_time, '%Y-%m') GROUP BY。")
    @GetMapping("/campus-monthly-summary")
    public Result<List<WaterMonthlySummaryVO>> campusMonthlySummary(
            @Parameter(description = "统计年份（YYYY，只查该年各月；不传则查全部）", example = "2026")
            @RequestParam(required = false) String statYear,
            @Parameter(description = "开始日期（yyyy-MM-dd）") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID（非ADMIN强制覆盖；不传=全园区）") @RequestParam(required = false) Long merchantId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<WaterMonthlySummaryVO> list = waterMeterReportService.getCampusMonthlySummary(
                statYear, startDate, endDate, merchantId);
        return Result.success(list);
    }

    @Operation(summary = "全园区年度用水量汇总（年度对比柱状图）",
            description = "管理层宏观视角：按自然年聚合全园区所有水表的总用水量与总费用。"
                    + "不区分具体水表或商户维度，返回按年升序排列的数据，"
                    + "适配前端图表库直接渲染年度对比柱状图或趋势图。"
                    + "X轴=statYear（YYYY），Y轴=totalUsage（吨）。"
                    + "数据来源：t_water_meter_reading 按 YEAR(record_time) GROUP BY。")
    @GetMapping("/campus-yearly-summary")
    public Result<List<WaterYearlySummaryVO>> campusYearlySummary(
            @Parameter(description = "开始日期（yyyy-MM-dd）") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID（非ADMIN强制覆盖；不传=全园区）") @RequestParam(required = false) Long merchantId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<WaterYearlySummaryVO> list = waterMeterReportService.getCampusYearlySummary(
                startDate, endDate, merchantId);
        return Result.success(list);
    }

    // ═══════════════════════════════════════════
    // 计费水量报表（财务视角：用水量 + 水费 + 平均单价）
    // ═══════════════════════════════════════════

    @Operation(summary = "计费水量汇总表（按天聚合，含平均单价）",
            description = "财务核费视角：按天聚合全园区用水量与水费总额，"
                    + "自动计算每日平均单价（totalFee ÷ totalUsage）。"
                    + "支持按日期范围、商户ID筛选。"
                    + "数据来源：t_water_meter_reading 同时 SUM(usage_amount) 和 SUM(fee)。")
    @GetMapping("/billing/daily")
    public Result<List<WaterBillingSummaryVO>> billingDaily(
            @Parameter(description = "开始日期（yyyy-MM-dd）") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID（非ADMIN强制覆盖；不传=全园区）") @RequestParam(required = false) Long merchantId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<WaterBillingSummaryVO> list = waterMeterReportService.getBillingDailySummary(
                startDate, endDate, merchantId);
        return Result.success(list);
    }

    @Operation(summary = "计费水量月报表（按月聚合，含平均单价）",
            description = "财务核费视角：按月聚合全园区用水量与水费总额，"
                    + "自动计算月平均单价，支持环比/同比分析。"
                    + "支持按月份、年份、商户ID筛选。"
                    + "数据来源：t_water_meter_reading 按 DATE_FORMAT(record_time, '%Y-%m') GROUP BY。")
    @GetMapping("/billing/monthly")
    public Result<List<WaterBillingMonthlyVO>> billingMonthly(
            @Parameter(description = "统计月份（YYYY-MM）", example = "2026-06")
            @RequestParam(required = false) String statMonth,
            @Parameter(description = "统计年份（YYYY，查整年各月）", example = "2026")
            @RequestParam(required = false) String statYear,
            @Parameter(description = "商户ID（非ADMIN强制覆盖；不传=全园区）") @RequestParam(required = false) Long merchantId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<WaterBillingMonthlyVO> list = waterMeterReportService.getBillingMonthlySummary(
                statMonth, statYear, merchantId);
        return Result.success(list);
    }

    @Operation(summary = "计费水量年报表（按年聚合，含平均单价）",
            description = "财务决算视角：按年聚合全园区用水量与水费总额，"
                    + "自动计算年平均单价，适合年度财务决算和水费预算编制。"
                    + "支持按日期范围、商户ID筛选。"
                    + "数据来源：t_water_meter_reading 按 YEAR(record_time) GROUP BY。")
    @GetMapping("/billing/yearly")
    public Result<List<WaterBillingYearlyVO>> billingYearly(
            @Parameter(description = "开始日期（yyyy-MM-dd）") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID（非ADMIN强制覆盖；不传=全园区）") @RequestParam(required = false) Long merchantId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<WaterBillingYearlyVO> list = waterMeterReportService.getBillingYearlySummary(
                startDate, endDate, merchantId);
        return Result.success(list);
    }

    // ──────── 权限辅助 ────────

    /**
     * Controller 层纵深防御：非 ADMIN 强制覆盖 merchantId。
     * @throws BusinessException 非 ADMIN 且未关联商户时抛出 403
     */
    private Long resolveMerchantIdForReport(Long requestedMerchantId) {
        if (!SecurityUserContext.isAdmin()) {
            Long myCustomerId = SecurityUserContext.getCurrentCustomerId();
            if (myCustomerId == null) {
                throw new BusinessException(403, "无权限：未关联商户");
            }
            return myCustomerId;
        }
        return requestedMerchantId;
    }
}
