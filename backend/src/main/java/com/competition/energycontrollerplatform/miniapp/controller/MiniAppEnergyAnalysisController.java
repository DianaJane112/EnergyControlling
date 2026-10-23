package com.competition.energycontrollerplatform.miniapp.controller;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.miniapp.dto.DailyTrendVO;
import com.competition.energycontrollerplatform.miniapp.dto.MonthlyTrendVO;
import com.competition.energycontrollerplatform.miniapp.security.MiniappSecurityContext;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppEnergyAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序 - 用能分析控制器。
 * <p>
 * 提供日用电趋势（月度每日）和月用电趋势（年度每月）的查询接口，
 * 供小程序 ECharts 折线图/柱状图渲染。
 * <p>
 * 用能概览数据由 {@code MiniAppDashboardController.dashboard()} 提供，
 * 本 Controller 专注趋势图表数据。
 */
@RestController
@RequestMapping("/api/v1/miniapp/energy-analysis")
@RequiredArgsConstructor
@Tag(name = "小程序-用能分析", description = "日用电趋势、月用电趋势图表数据")
public class MiniAppEnergyAnalysisController {

    private final MiniAppEnergyAnalysisService energyAnalysisService;

    @GetMapping("/daily-electric-trend")
    @Operation(summary = "日用电趋势", description = "查询指定月份内每日用电量，返回完整月度数据序列（缺失日期填0）。"
            + "数据来源：tb_meter_daily_report 预聚合表，无数据时降级查询 t_meter_reading。")
    public Result<List<DailyTrendVO>> dailyElectricTrend(
            @Parameter(description = "月份（yyyy-MM），默认当月", example = "2026-06")
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long meterId) {
        Long userId = MiniappSecurityContext.getCurrentUserId();
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        List<DailyTrendVO> trends = energyAnalysisService.getDailyElectricTrend(userId, customerId, meterId, month);
        return Result.success(trends);
    }

    @GetMapping("/monthly-electric-trend")
    @Operation(summary = "月用电趋势", description = "查询指定年份内每月用电量，返回完整年度数据序列（缺失月份填0）。"
            + "数据来源：tb_meter_monthly_report 预聚合表，无数据时降级查询 t_meter_reading。")
    public Result<List<MonthlyTrendVO>> monthlyElectricTrend(
            @Parameter(description = "年份（yyyy），默认当年", example = "2026")
            @RequestParam(required = false) String year,
            @RequestParam(required = false) Long meterId) {
        Long userId = MiniappSecurityContext.getCurrentUserId();
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        List<MonthlyTrendVO> trends = energyAnalysisService.getMonthlyElectricTrend(userId, customerId, meterId, year);
        return Result.success(trends);
    }
}
