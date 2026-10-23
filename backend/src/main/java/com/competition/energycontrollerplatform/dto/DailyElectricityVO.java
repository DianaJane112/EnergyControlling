package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用电量日报表响应 VO。
 */
@Data
@Schema(description = "用电量日报表条目")
public class DailyElectricityVO {

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "设备号（表钢印号/表通讯地址）")
    private String deviceNo;

    @Schema(description = "商户ID")
    private Long customerId;

    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "分组ID")
    private Long groupId;

    @Schema(description = "分组名称")
    private String groupName;

    // ── 正向/反向有功 ──

    @Schema(description = "正向有功日用电量（kWh）")
    private BigDecimal forwardConsumed;

    @Schema(description = "反向有功日用电量（kWh）")
    private BigDecimal reverseConsumed;

    @Schema(description = "反向占比（%）= 反向有功 / (正向+反向) × 100")
    private BigDecimal reverseRatio;

    // ── 分时电量 ──

    @Schema(description = "当日用电量（kWh）")
    private BigDecimal electricityConsumed;

    @Schema(description = "尖时段用电量（kWh）")
    private BigDecimal peakConsumed;

    @Schema(description = "峰时段用电量（kWh）")
    private BigDecimal busyConsumed;

    @Schema(description = "平时段用电量（kWh）")
    private BigDecimal normalConsumed;

    @Schema(description = "谷时段用电量（kWh）")
    private BigDecimal idleConsumed;

    @Schema(description = "尖时段电费（元）")
    private BigDecimal peakChargeAmount;

    @Schema(description = "峰时段电费（元）")
    private BigDecimal busyChargeAmount;

    @Schema(description = "平时段电费（元）")
    private BigDecimal normalChargeAmount;

    @Schema(description = "谷时段电费（元）")
    private BigDecimal idleChargeAmount;

    @Schema(description = "当日扣费金额（元）")
    private BigDecimal chargeAmount;

    // ── 虚拟表 ──

    @Schema(description = "关联虚拟表计ID（来自 t_shadow_meter）")
    private Long virtualMeterId;

    @Schema(description = "虚拟表号")
    private String virtualMeterNo;

    @Schema(description = "虚拟表商户名")
    private String virtualMerchantName;
}
