package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分账统计响应 VO（按日/月/年聚合资金流水统计）。
 * <p>
 * 聚合维度：充值、扣费、退费、调账、净流水、流水笔数。
 * 所有金额字段均为 {@link BigDecimal}。
 */
@Data
@Schema(description = "分账统计条目（按日/月/年聚合）")
public class AccountFlowSummaryVO {

    @Schema(description = "统计时段（日:yyyy-MM-dd, 月:yyyy-MM, 年:yyyy）")
    private String period;

    @Schema(description = "充值总额（元，含充值+人工缴费）")
    private BigDecimal totalRecharge;

    @Schema(description = "扣费总额（元，消费扣费绝对值）")
    private BigDecimal totalDebit;

    @Schema(description = "退费总额（元）")
    private BigDecimal totalRefund;

    @Schema(description = "调账总额（元，正=调增，负=调减）")
    private BigDecimal totalAdjust;

    @Schema(description = "公共分摊总额（元）")
    private BigDecimal totalAllocation;

    @Schema(description = "净流水（元，正=净流入）")
    private BigDecimal netFlow;

    @Schema(description = "流水笔数")
    private Long flowCount;
}
