package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 余额调整统计聚合响应 VO。
 * <p>
 * 按日聚合余额调整总金额、笔数、涉及商户数。
 * 所有金额字段均为 {@link BigDecimal}。
 */
@Data
@Schema(description = "余额调整统计聚合条目")
public class BalanceAdjustmentSummaryVO {

    @Schema(description = "统计时段（日:yyyy-MM-dd）")
    private String period;

    @Schema(description = "该时段调整总金额（元）")
    private BigDecimal totalAmount;

    @Schema(description = "该时段调整笔数")
    private Long adjustCount;

    @Schema(description = "该时段涉及商户数")
    private Long customerCount;
}
