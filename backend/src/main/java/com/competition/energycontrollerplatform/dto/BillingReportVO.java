package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计费金额报表响应 VO（§五.8/9/10，支持电费+水费）。
 * <p>按日/月/年维度聚合计费扣款金额与用量，自动计算平均单价。</p>
 */
@Data
@Schema(description = "计费金额报表条目")
public class BillingReportVO {

    @Schema(description = "统计时段（日/月/年）")
    private String period;

    @Schema(description = "总扣费金额（元）")
    private BigDecimal totalChargeAmount;

    @Schema(description = "总用电量（kWh）")
    private BigDecimal totalConsumed;

    @Schema(description = "涉及表计数")
    private Long meterCount;

    @Schema(description = "平均电价（元/kWh）")
    private BigDecimal averagePrice;
}
