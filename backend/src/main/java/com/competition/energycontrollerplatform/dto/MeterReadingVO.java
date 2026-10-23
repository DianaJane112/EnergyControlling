package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 表计实时读数响应 VO
 */
@Data
@Schema(description = "表计实时读数响应")
public class MeterReadingVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联表计ID")
    private Long meterId;

    @Schema(description = "采集时间戳（毫秒）")
    private Long timestamp;

    // ── 电压 ──
    @Schema(description = "A相电压（V）")
    private BigDecimal voltA;

    @Schema(description = "B相电压（V）")
    private BigDecimal voltB;

    @Schema(description = "C相电压（V）")
    private BigDecimal voltC;

    // ── 电流 ──
    @Schema(description = "A相电流（A）")
    private BigDecimal ampA;

    @Schema(description = "B相电流（A）")
    private BigDecimal ampB;

    @Schema(description = "C相电流（A）")
    private BigDecimal ampC;

    // ── 有功功率 ──
    @Schema(description = "总有功功率（W）")
    private BigDecimal act;

    @Schema(description = "A相有功功率（W）")
    private BigDecimal actA;

    @Schema(description = "B相有功功率（W）")
    private BigDecimal actB;

    @Schema(description = "C相有功功率（W）")
    private BigDecimal actC;

    // ── 累计电量 ──
    @Schema(description = "组合有功总电量（kWh）")
    private BigDecimal kwh;

    @Schema(description = "尖电量（kWh）")
    private BigDecimal kwhPeak;

    @Schema(description = "峰电量（kWh）")
    private BigDecimal kwhBusy;

    @Schema(description = "平电量（kWh）")
    private BigDecimal kwhNormal;

    @Schema(description = "谷电量（kWh）")
    private BigDecimal kwhIdle;

    // ── 功率因数 ──
    @Schema(description = "总功率因数")
    private BigDecimal fact;

    private BigDecimal factA;
    private BigDecimal factB;
    private BigDecimal factC;
    private BigDecimal react;
    private BigDecimal reactA;
    private BigDecimal reactB;
    private BigDecimal reactC;

    @Schema(description = "电网频率（Hz）")
    private BigDecimal freq;
}
