package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计费水量汇总表 VO（按天聚合）。
 * <p>
 * <b>财务视角：</b>核心关注"用了多少水 → 产生了多少水费"，
 * 自动计算平均单价（总费用 ÷ 总用量），方便财务核对费率是否异常。
 * </p>
 * <p>
 * 数据来源：t_water_meter_reading 按 DATE(record_time) GROUP BY。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "计费水量汇总表（按天）")
public class WaterBillingSummaryVO {

    @Schema(description = "统计日期（yyyy-MM-dd）", example = "2026-06-07")
    private String statDate;

    @Schema(description = "日总用水量（吨）", example = "420.50")
    private BigDecimal totalUsage;

    @Schema(description = "日总水费（元）", example = "1429.70")
    private BigDecimal totalFee;

    @Schema(description = "平均单价（元/吨）= totalFee ÷ totalUsage，财务核费核心指标", example = "3.40")
    private BigDecimal averagePrice;

    @Schema(description = "当日活跃水表数", example = "48")
    private Integer meterCount;
}
