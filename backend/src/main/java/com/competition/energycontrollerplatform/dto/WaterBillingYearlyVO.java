package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计费水量年报表 VO（按年聚合）。
 * <p>
 * <b>财务视角：</b>按年统计全园区用水量与水费总额，
 * 自动计算年平均单价。适合年度财务决算和水费预算编制。
 * </p>
 * <p>
 * 数据来源：t_water_meter_reading 按 YEAR(record_time) GROUP BY。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "计费水量年报表")
public class WaterBillingYearlyVO {

    @Schema(description = "统计年份（yyyy）", example = "2026")
    private String statYear;

    @Schema(description = "年总用水量（吨）", example = "145320.80")
    private BigDecimal totalUsage;

    @Schema(description = "年总水费（元）", example = "494091.72")
    private BigDecimal totalFee;

    @Schema(description = "年平均单价（元/吨）= totalFee ÷ totalUsage", example = "3.40")
    private BigDecimal averagePrice;

    @Schema(description = "年内活跃水表数（去重）", example = "52")
    private Integer meterCount;

    @Schema(description = "年内有读数记录的月数", example = "12")
    private Integer monthCount;
}
