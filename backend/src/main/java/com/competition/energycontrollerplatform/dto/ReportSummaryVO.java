package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 报表汇总响应 VO（日/月/年汇总通用）。
 */
@Data
@Schema(description = "报表汇总条目")
public class ReportSummaryVO {

    @Schema(description = "统计时段")
    private String period;

    @Schema(description = "总用电量（kWh）")
    private BigDecimal totalConsumed;

    @Schema(description = "涉及表计数")
    private Long meterCount;
}
