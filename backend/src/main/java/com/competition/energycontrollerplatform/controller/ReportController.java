package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.BillingReportVO;
import com.competition.energycontrollerplatform.dto.CustomerComprehensiveVO;
import com.competition.energycontrollerplatform.dto.DailyElectricityVO;
import com.competition.energycontrollerplatform.dto.ElectricLoadCurveVO;
import com.competition.energycontrollerplatform.dto.ElectricLoadVO;
import com.competition.energycontrollerplatform.dto.MonthlyElecSummaryVO;
import com.competition.energycontrollerplatform.dto.MonthlyMeterDetailVO;
import com.competition.energycontrollerplatform.dto.ReportSummaryVO;
import com.competition.energycontrollerplatform.dto.TimeOfUseReportVO;
import com.competition.energycontrollerplatform.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表统计 Controller。
 * <p>
 * 对应需求文档 §五「报表统计」1-11类报表。
 * 所有查询直读预聚合表 t_report_daily_electricity。
 */
@Tag(name = "统计报表（电表）", description = "用电量日报/月报/年报、复费率报表、客户综合报表查询接口")
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ──────────────── 用电量日报表（§五.1/2/3） ────────────────

    @Operation(summary = "用电量日报表明细（§五.1）", description = "按总用户/分组/分户维度查询每日用电量明细。"
            + "数据来源：t_report_daily_electricity 预聚合表。")
    @GetMapping("/daily-electricity")
    public Result<IPage<DailyElectricityVO>> dailyElectricity(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        IPage<DailyElectricityVO> page = reportService.listDailyElectricity(
                pageNum, pageSize, startDate, endDate, customerId, groupId);
        return Result.success(page);
    }

    // ──────────────── 用电量日报汇总（§五.1/2/3 汇总视图） ────────────────

    @Operation(summary = "用电量日报汇总", description = "按日聚合总用电量。支持总用户/分组/分户三维筛选。")
    @GetMapping("/daily-summary")
    public Result<List<ReportSummaryVO>> dailySummary(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<ReportSummaryVO> summary = reportService.sumDailyElectricity(startDate, endDate, customerId, groupId);
        return Result.success(summary);
    }

    // ──────────────── 用电量月报表（§五.6/7） ────────────────

    @Operation(summary = "用电量月报表汇总（§五.6）", description = "按月聚合总用电量。支持总用户/分组/分户三维筛选。")
    @GetMapping("/monthly-summary")
    public Result<List<ReportSummaryVO>> monthlySummary(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<ReportSummaryVO> summary = reportService.sumMonthlyElectricity(startDate, endDate, customerId, groupId);
        return Result.success(summary);
    }

    // ──────────────── 用电量月报表明细（按表计+每日用量） ────────────────

    @Operation(summary = "用电量月报表明细（按表计维度展开每日用量）", description = "按表计维度查询某月内每日用电量明细。"
            + "返回各表计的维度信息（设备类型、分组、商户、虚拟表）及每日用量 Map，前端据此动态生成日列。")
    @GetMapping("/monthly-detail")
    public Result<List<MonthlyMeterDetailVO>> monthlyDetail(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<MonthlyMeterDetailVO> detail = reportService.listMonthlyMeterDetail(
                startDate, endDate, customerId, groupId);
        return Result.success(detail);
    }

    // ──────────────── 月度用电量汇总明细（按表计×月度，含正向/反向/余额） ────────────────

    @Operation(summary = "月度用电量汇总明细", description = "按表计×月度维度查询正向/反向有功电量起止示数、总起止示数、余额等完整字段。"
            + "数据来源：tb_meter_monthly_report 预聚合月报表。")
    @GetMapping("/monthly-elec-summary")
    public Result<List<MonthlyElecSummaryVO>> monthlyElecSummary(
            @Parameter(description = "统计月份（yyyy-MM）") @RequestParam(required = false) String statMonth,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<MonthlyElecSummaryVO> report = reportService.listMonthlyElecSummary(statMonth, customerId, groupId);
        return Result.success(report);
    }

    // ──────────────── 客户综合报表（§五.11） ────────────────

    @Operation(summary = "客户综合报表（§五.11）", description = "汇总每个商户的总充值、总用量、总费用、当前余额。"
            + "支持按日期范围筛选。")
    @GetMapping("/customer-comprehensive")
    public Result<List<CustomerComprehensiveVO>> customerComprehensive(
            @Parameter(description = "开始日期", example = "2026-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期", example = "2026-05-31") @RequestParam(required = false) String endDate,
            @Parameter(description = "商户ID（ADMIN可查全量，非ADMIN强制覆盖）") @RequestParam(required = false) Long customerId) {
        List<CustomerComprehensiveVO> report = reportService.getCustomerComprehensiveReport(startDate, endDate, customerId);
        return Result.success(report);
    }

    // ──────────────── 用电量年报表（§五.7） ────────────────

    @Operation(summary = "用电量年报表汇总（§五.7）", description = "按年聚合总用电量。支持总用户/分组/分户三维筛选。")
    @GetMapping("/yearly-summary")
    public Result<List<ReportSummaryVO>> yearlySummary(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<ReportSummaryVO> summary = reportService.sumYearlyElectricity(startDate, endDate, customerId, groupId);
        return Result.success(summary);
    }

    // ──────────────── 复费率分时日报（§五.4） ────────────────

    @Operation(summary = "复费率计费电量日报表（§五.4）", description = "按日聚合尖/峰/平/谷分时用电量。"
            + "数据来源：t_report_daily_electricity 预聚合表 peak_consumed/busy_consumed/normal_consumed/idle_consumed 列。")
    @GetMapping("/time-of-use/daily")
    public Result<List<TimeOfUseReportVO>> timeOfUseDaily(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<TimeOfUseReportVO> report = reportService.sumTimeOfUseDaily(startDate, endDate, customerId, groupId);
        return Result.success(report);
    }

    @Operation(summary = "复费率计费电量日报表明细（§五.4明细）", description = "按meter×date维度展开尖/峰/平/谷分时用电量及电费明细。"
            + "包含商户号、表号，用于明细表格展示。")
    @GetMapping("/time-of-use/daily-detail")
    public Result<List<TimeOfUseReportVO>> timeOfUseDailyDetail(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<TimeOfUseReportVO> report = reportService.sumTimeOfUseDailyDetail(startDate, endDate, customerId, groupId);
        return Result.success(report);
    }

    // ──────────────── 复费率分时月报（§五.5） ────────────────

    @Operation(summary = "复费率计费电量汇总表（§五.5）", description = "按月聚合尖/峰/平/谷分时用电量。"
            + "前端可用于渲染分时柱状图/饼图。")
    @GetMapping("/time-of-use/monthly")
    public Result<List<TimeOfUseReportVO>> timeOfUseMonthly(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<TimeOfUseReportVO> report = reportService.sumTimeOfUseMonthly(startDate, endDate, customerId, groupId);
        return Result.success(report);
    }

    // ──────────────── 计费金额日报（§五.8） ────────────────

    @Operation(summary = "计费电量汇总表（§五.8）", description = "按日聚合计费扣款金额与用电量，自动计算平均电价。")
    @GetMapping("/billing/daily")
    public Result<List<BillingReportVO>> billingDaily(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<BillingReportVO> report = reportService.sumBillingDaily(startDate, endDate, customerId, groupId);
        return Result.success(report);
    }

    // ──────────────── 计费金额月报（§五.9） ────────────────

    @Operation(summary = "计费电量月报表（§五.9）", description = "按月聚合计费扣款金额与用电量，自动计算平均电价。")
    @GetMapping("/billing/monthly")
    public Result<List<BillingReportVO>> billingMonthly(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<BillingReportVO> report = reportService.sumBillingMonthly(startDate, endDate, customerId, groupId);
        return Result.success(report);
    }

    // ──────────────── 计费金额年报（§五.10） ────────────────

    @Operation(summary = "计费电量年报表（§五.10）", description = "按年聚合计费扣款金额与用电量，自动计算平均电价。")
    @GetMapping("/billing/yearly")
    public Result<List<BillingReportVO>> billingYearly(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<BillingReportVO> report = reportService.sumBillingYearly(startDate, endDate, customerId, groupId);
        return Result.success(report);
    }

    // ──────────────── 查询用电负荷（§五.12） ────────────────

    @Operation(summary = "查询用电负荷（§五.12）", description = "按日/月维度查询最大负荷、平均负荷、最小负荷及负荷率。"
            + "负荷率 = 平均负荷 / 最大负荷 × 100%，反映用电均衡程度。"
            + "数据来源：t_report_daily_electricity 预聚合表，对各表计每日用电量做 max/avg/min 聚合。")
    @GetMapping("/electric-load")
    public Result<List<ElectricLoadVO>> electricLoad(
            @Parameter(description = "聚合粒度：daily/monthly，默认 daily", example = "daily")
            @RequestParam(defaultValue = "daily") String granularity,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<ElectricLoadVO> report;
        if ("monthly".equals(granularity)) {
            report = reportService.getMonthlyElectricLoad(startDate, endDate, customerId, groupId);
        } else {
            report = reportService.getDailyElectricLoad(startDate, endDate, customerId, groupId);
        }
        return Result.success(report);
    }

    // ──────────────── 用电负荷曲线图（§五.12 曲线图） ────────────────

    @Operation(summary = "用电负荷曲线图（§五.12曲线图）", description = "查询每日用电负荷曲线数据，按日期升序返回。"
            + "每条记录包含当日总用电量及尖/峰/平/谷各时段用电量，前端可直接用于渲染 ECharts 折线图/面积图。"
            + "数据来源：t_report_daily_electricity 预聚合表。")
    @GetMapping("/electric-load-curve")
    public Result<List<ElectricLoadCurveVO>> electricLoadCurve(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID（非ADMIN强制覆盖）") @RequestParam(required = false) Long customerId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        List<ElectricLoadCurveVO> report = reportService.getElectricLoadCurve(
                startDate, endDate, customerId, groupId);
        return Result.success(report);
    }
}
