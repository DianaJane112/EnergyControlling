package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计费电量年报表明细 VO（按表计+年度/本月汇总宽表）。
 * <p>
 * 前端列：序号 | 商户名 | 表号 | 虚拟表号 | 虚拟表商户名 |
 * 当年年度（金额/用量）| 本月（金额/用量/本期余额/上期余额）
 */
@Data
@Schema(description = "计费电量年报条目（按表计+年度/本月汇总）")
public class BillingYearlyMeterDetailVO {

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

    // ── 当年年度 ──

    @Schema(description = "年度计费金额（元）")
    private BigDecimal yearAmount = BigDecimal.ZERO;

    @Schema(description = "年度用电量（kWh）")
    private BigDecimal yearUsage = BigDecimal.ZERO;

    // ── 本月 ──

    @Schema(description = "本月计费金额（元）")
    private BigDecimal monthAmount = BigDecimal.ZERO;

    @Schema(description = "本月用电量（kWh）")
    private BigDecimal monthUsage = BigDecimal.ZERO;

    @Schema(description = "本期余额（元）")
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Schema(description = "上期余额（元）")
    private BigDecimal previousBalance = BigDecimal.ZERO;
}
