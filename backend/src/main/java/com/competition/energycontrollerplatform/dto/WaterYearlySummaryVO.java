package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 全园区年度用水量汇总 VO。
 * <p>
 * <b>业务视角：物业管理层</b> — 按自然年统计整个园区的用水总量趋势。
 * 数据结构设计为前端图表友好：{@code statYear} 作 X 轴，{@code totalUsage} 作 Y 轴，
 * 可直接渲染年度对比柱状图或趋势折线图。
 * </p>
 * <p>
 * 数据来源：t_water_meter_reading 按 {@code YEAR(record_time)} GROUP BY。
 * </p>
 *
 * @author EnergyControllerPlatform
 */
@Data
@Schema(description = "全园区年度用水量汇总（图表友好）")
public class WaterYearlySummaryVO {

    @Schema(description = "统计年份（yyyy），前端 X 轴", example = "2026")
    private String statYear;

    @Schema(description = "年度总用水量（吨），前端 Y 轴", example = "145320.80")
    private BigDecimal totalUsage;

    @Schema(description = "年度总费用（元），前端可选 Y2 轴", example = "494091.72")
    private BigDecimal totalFee;

    @Schema(description = "年内活跃水表数（去重）", example = "52")
    private Integer meterCount;

    @Schema(description = "年内有读数记录的月数", example = "12")
    private Integer monthCount;
}
