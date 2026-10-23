package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计费电量汇总表（按表计维度）VO。
 * <p>
 * 前端列：序号 | 商户名 | 计费均价(元/度) | 表号 | 虚拟表号 | 虚拟表商户名 | 计费金额(元) | 计费电量(kWh)
 */
@Data
@Schema(description = "计费电量汇总条目（按表计维度）")
public class BillingMeterSummaryVO {

    @Schema(description = "表计ID")
    private Long meterId;

    @Schema(description = "商户名称", example = "天辰超市")
    private String customerName;

    @Schema(description = "计费均价（元/度）= 计费金额 / 计费电量", example = "0.85")
    private BigDecimal averagePrice;

    @Schema(description = "表号/设备号", example = "CM2024001")
    private String deviceNo;

    @Schema(description = "虚拟表号")
    private String virtualMeterNo;

    @Schema(description = "虚拟表商户名")
    private String virtualMerchantName;

    @Schema(description = "计费金额（元）")
    private BigDecimal totalChargeAmount;

    @Schema(description = "计费电量（kWh）")
    private BigDecimal totalConsumed;
}
