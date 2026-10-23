package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 全园区月度用水量汇总 VO。
 * <p>
 * <b>业务视角：物业管理层</b> — 不区分具体水表/商户，按自然月统计整个园区的用水总量趋势。
 * 数据结构设计为前端图表友好：{@code statMonth} 作 X 轴，{@code totalUsage} 作 Y 轴，
 * 可直接渲染折线图或柱状图。
 * </p>
 * <p>
 * 数据来源：t_water_meter_reading 按 {@code DATE_FORMAT(record_time, '%Y-%m')} GROUP BY。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "全园区月度用水量汇总（图表友好）")
public class WaterMonthlySummaryVO {

    @Schema(description = "统计月份（yyyy-MM），前端 X 轴", example = "2026-01")
    private String statMonth;

    @Schema(description = "月度总用水量（吨），前端 Y 轴", example = "12580.50")
    private BigDecimal totalUsage;

    @Schema(description = "月度总费用（元），前端可选 Y2 轴", example = "42773.70")
    private BigDecimal totalFee;

    @Schema(description = "当月活跃水表数", example = "48")
    private Integer meterCount;

    @Schema(description = "当月有读数记录的天数", example = "31")
    private Integer dailyCount;
}
