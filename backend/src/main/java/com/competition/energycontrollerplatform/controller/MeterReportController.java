package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.BillingMeterSummaryVO;
import com.competition.energycontrollerplatform.dto.BillingMonthlyMeterVO;
import com.competition.energycontrollerplatform.dto.BillingReportVO;
import com.competition.energycontrollerplatform.dto.BillingYearlyMeterDetailVO;
import com.competition.energycontrollerplatform.dto.DeletedMeterReportVO;
import com.competition.energycontrollerplatform.dto.ReportSummaryVO;
import com.competition.energycontrollerplatform.dto.YearlyMeterDetailVO;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
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
 * 表计综合报表 Controller（水+电统一，按设备维度）。
 * <p>
 * 对应前端 meterReport.js API 模块。
 * 电表报表走预聚合表 t_report_daily_electricity / tb_meter_monthly_report。
 */
@Tag(name = "表计综合报表", description = "按设备维度查询日报/月报/年报、已删除表计月报、计费报表")
@RestController
@RequestMapping("/api/v1/meter-report")
@RequiredArgsConstructor
public class MeterReportController {

    private final ReportService reportService;

    // ──────────────── 已删除表计月报（前端 elec-monthly-deleted） ────────────────

    @Operation(summary = "已删除表计月报表", description = "查询已逻辑删除的电表在指定月份的历史用电汇总，支持按商户/分组筛选。"
            + "数据来源：tb_meter_monthly_report JOIN t_meter（is_deleted=1）。")
    @GetMapping("/deleted-meter-monthly")
    public Result<List<DeletedMeterReportVO>> deletedMeterMonthly(
            @Parameter(description = "统计月份（yyyy-MM）") @RequestParam(required = false) String statMonth,
            @Parameter(description = "商户ID（与前端 customerId 对齐）") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<DeletedMeterReportVO> report = reportService.getDeletedMeterMonthlyReport(statMonth, merchantId, groupId);
        return Result.success(report);
    }

    // ──────────────── 月报汇总（按月聚合，前端月度/年度用电量汇总表） ────────────────

    @Operation(summary = "表计月报表汇总", description = "按月聚合总用电量。")
    @GetMapping("/monthly-summary")
    public Result<List<ReportSummaryVO>> monthlySummary(
            @Parameter(description = "统计月份（yyyy-MM）") @RequestParam(required = false) String statMonth,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId,
            @Parameter(description = "项目ID") @RequestParam(required = false) Long projectId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<ReportSummaryVO> summary = reportService.sumMonthlyElectricity(startDate, endDate, merchantId, groupId);
        return Result.success(summary);
    }

    // ──────────────── 年报表汇总 ────────────────

    @Operation(summary = "表计年报表汇总", description = "按年聚合总用电量。")
    @GetMapping("/yearly-summary")
    public Result<List<ReportSummaryVO>> yearlySummary(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId,
            @Parameter(description = "项目ID") @RequestParam(required = false) Long projectId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<ReportSummaryVO> summary = reportService.sumYearlyElectricity(startDate, endDate, merchantId, groupId);
        return Result.success(summary);
    }

    // ──────────────── 年度用电量汇总（按表计×12月明细） ────────────────

    @Operation(summary = "年度用电量汇总表（按表计×12月明细）", description = "按表计维度查询指定年份1~12月用电量分列及小计。"
            + "数据来源：t_report_daily_electricity 按月条件聚合。")
    @GetMapping("/yearly-detail")
    public Result<List<YearlyMeterDetailVO>> yearlyDetail(
            @Parameter(description = "统计年份（yyyy）") @RequestParam(required = false) String year,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        // 默认当前年份
        String effectiveYear = (year != null && !year.isEmpty()) ? year : String.valueOf(java.time.Year.now().getValue());
        List<YearlyMeterDetailVO> detail = reportService.getYearlyMeterDetail(effectiveYear, merchantId, groupId);
        return Result.success(detail);
    }

    // ──────────────── 计费电量年报明细（按表计+年度/本月汇总宽表） ────────────────

    @Operation(summary = "计费电量年报明细（按表计+年度/本月汇总宽表）",
            description = "每个表计一行，包含当前年度计费金额/用电量合计、"
                    + "当前月度计费金额/用电量合计、以及当月余额信息。"
                    + "前端展示两级表头宽表：序号 | 商户名 | 表号 | 虚拟表号 | 虚拟表商户名 | "
                    + "当年年度(金额/用量) | 本月(金额/用量/本期余额/上期余额)。")
    @GetMapping("/billing-yearly-detail")
    public Result<List<BillingYearlyMeterDetailVO>> billingYearlyDetail(
            @Parameter(description = "统计年份（yyyy，默认当前年份）")
                @RequestParam(required = false) String year,
            @Parameter(description = "统计月份（yyyy-MM，默认当前月份）")
                @RequestParam(required = false) String statMonth,
            @Parameter(description = "商户ID")
                @RequestParam(required = false) Long merchantId,
            @Parameter(description = "分组ID")
                @RequestParam(required = false) Long groupId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        java.time.YearMonth current = java.time.YearMonth.now();
        String effectiveYear = (year != null && !year.isEmpty())
                ? year : String.valueOf(current.getYear());
        String effectiveMonth = (statMonth != null && !statMonth.isEmpty())
                ? statMonth : current.toString();
        List<BillingYearlyMeterDetailVO> detail = reportService.getBillingYearlyMeterDetail(
                effectiveYear, effectiveMonth, merchantId, groupId);
        return Result.success(detail);
    }

    // ──────────────── 计费电量汇总（按表计维度） ────────────────

    @Operation(summary = "计费电量汇总表（按表计维度）", description = "按表计维度聚合指定日期范围内的计费金额与用电量，含平均电价和虚拟表信息。")
    @GetMapping("/billing/meter-summary")
    public Result<List<BillingMeterSummaryVO>> billingMeterSummary(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<BillingMeterSummaryVO> summary = reportService.getBillingMeterSummary(startDate, endDate, merchantId, groupId);
        return Result.success(summary);
    }

    // ──────────────── 计费电量月报表明细（按表计+每日计费） ────────────────

    @Operation(summary = "计费电量月报表明细（按表计+每日计费）", description = "按表计维度展开当月每日的计费电量与计费金额，"
            + "包含设备类型、分组、商户、虚拟表等维度信息。前端据此动态生成日列（二级表头：日期→总电量/总费用）和汇总列。")
    @GetMapping("/billing/monthly-meter-detail")
    public Result<List<BillingMonthlyMeterVO>> billingMonthlyMeterDetail(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "分组ID") @RequestParam(required = false) Long groupId) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<BillingMonthlyMeterVO> detail = reportService.getBillingMonthlyMeterDetail(startDate, endDate, merchantId, groupId);
        return Result.success(detail);
    }

    // ──────────────── 计费报表 ────────────────

    @Operation(summary = "计费汇总表（按日）", description = "按日聚合计费金额与用电量，含平均电价。")
    @GetMapping("/billing/daily")
    public Result<List<BillingReportVO>> billingDaily(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "表计类型：ELECTRIC/WATER") @RequestParam(required = false) String meterType) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<BillingReportVO> report = reportService.sumBillingDaily(startDate, endDate, merchantId, null);
        return Result.success(report);
    }

    @Operation(summary = "计费月报表", description = "按月聚合计费金额与用电量，含平均电价。")
    @GetMapping("/billing/monthly")
    public Result<List<BillingReportVO>> billingMonthly(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "表计类型：ELECTRIC/WATER") @RequestParam(required = false) String meterType) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<BillingReportVO> report = reportService.sumBillingMonthly(startDate, endDate, merchantId, null);
        return Result.success(report);
    }

    @Operation(summary = "计费年报表", description = "按年聚合计费金额与用电量，含平均电价。")
    @GetMapping("/billing/yearly")
    public Result<List<BillingReportVO>> billingYearly(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "商户ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "表计类型：ELECTRIC/WATER") @RequestParam(required = false) String meterType) {
        merchantId = resolveMerchantIdForReport(merchantId);
        List<BillingReportVO> report = reportService.sumBillingYearly(startDate, endDate, merchantId, null);
        return Result.success(report);
    }

    // ──────── 权限辅助 ────────

    /**
     * 解析报表查询的商户 ID，非 ADMIN 强制覆盖为当前用户关联的商户 ID。
     * <p>这是 controller 层纵深防御：即使 service 层漏做了权限隔离，controller 层也会拦截。</p>
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
