package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户综合报表响应 VO（对应需求文档 §五.11）。
 * <p>汇总：总充值、总用量、总费用、当前余额。
 */
@Data
@Schema(description = "客户综合报表条目")
public class CustomerComprehensiveVO {

    @Schema(description = "商户ID")
    private Long customerId;

    @Schema(description = "商户名称")
    private String customerName;

    @Schema(description = "总充值金额（元）")
    private BigDecimal totalRecharge;

    @Schema(description = "总用电量（kWh）")
    private BigDecimal totalConsumed;

    @Schema(description = "总扣费金额（元）")
    private BigDecimal totalCharge;

    @Schema(description = "当前余额（元）")
    private BigDecimal currentBalance;
}
