package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度用电量汇总明细 VO。
 * <p>
 * 按表计 × 月份维度展示正向/反向有功电量起止示数及用量、
 * 总起止示数、余额等完整字段。用于前端月度用电量汇总表。
 */
@Data
@Schema(description = "月度用电量汇总明细条目（按表计×月份）")
public class MonthlyElecSummaryVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "表号（设备号）", example = "CM2024001")
    private String deviceNo;

    @Schema(description = "商户名称", example = "某某科技有限公司")
    private String customerName;

    @Schema(description = "分组名称", example = "B13栋")
    private String groupName;

    @Schema(description = "虚拟表号")
    private String virtualMeterNo;

    @Schema(description = "虚拟表商户名")
    private String virtualMerchantName;

    @Schema(description = "表计类型", example = "租户")
    private String meterType;

    @Schema(description = "统计月份（yyyy-MM）", example = "2026-06")
    private String statMonth;

    // ── 正向有功电量 ──

    @Schema(description = "正向有功上期示数")
    private BigDecimal forwardStartReading;

    @Schema(description = "正向有功上期时间")
    private LocalDateTime forwardStartTime;

    @Schema(description = "正向有功本期示数")
    private BigDecimal forwardEndReading;

    @Schema(description = "正向有功本期时间")
    private LocalDateTime forwardEndTime;

    @Schema(description = "正向有功用量")
    private BigDecimal forwardUsage;

    // ── 反向有功电量 ──

    @Schema(description = "反向有功上期示数")
    private BigDecimal reverseStartReading;

    @Schema(description = "反向有功上期时间")
    private LocalDateTime reverseStartTime;

    @Schema(description = "反向有功本期示数")
    private BigDecimal reverseEndReading;

    @Schema(description = "反向有功本期时间")
    private LocalDateTime reverseEndTime;

    @Schema(description = "反向有功用量")
    private BigDecimal reverseUsage;

    // ── 反向占比 ──

    @Schema(description = "反向占比(%)")
    private BigDecimal reverseRatio;

    // ── 总起止示数 ──

    @Schema(description = "总起示数")
    private BigDecimal totalStartReading;

    @Schema(description = "总止示数")
    private BigDecimal totalEndReading;

    // ── 总电量 ──

    @Schema(description = "总电量（kWh）")
    private BigDecimal totalEnergy;

    // ── 余额 ──

    @Schema(description = "上期余额")
    private BigDecimal previousBalance;

    @Schema(description = "本期余额")
    private BigDecimal currentBalance;

    // ── 数据更新时间 ──

    @Schema(description = "数据更新时间")
    private LocalDateTime dataUpdateTime;
}
