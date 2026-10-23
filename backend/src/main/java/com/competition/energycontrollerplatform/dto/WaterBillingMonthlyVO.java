package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计费水量月报表 VO（按月聚合）。
 * <p>
 * <b>财务视角：</b>按月统计全园区用水量与水费总额，
 * 自动计算月平均单价。支持与前月/去年同期对比分析。
 * </p>
 * <p>
 * 数据来源：t_water_meter_reading 按 DATE_FORMAT(record_time, '%Y-%m') GROUP BY。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "计费水量月报表")
public class WaterBillingMonthlyVO {

    @Schema(description = "统计月份（yyyy-MM）", example = "2026-06")
    private String statMonth;

    @Schema(description = "月总用水量（吨）", example = "12580.50")
    private BigDecimal totalUsage;

    @Schema(description = "月总水费（元）", example = "42773.70")
    private BigDecimal totalFee;

    @Schema(description = "月平均单价（元/吨）= totalFee ÷ totalUsage", example = "3.40")
    private BigDecimal averagePrice;

    @Schema(description = "当月活跃水表数", example = "48")
    private Integer meterCount;

    @Schema(description = "当月有读数记录的天数", example = "30")
    private Integer dailyCount;
}
