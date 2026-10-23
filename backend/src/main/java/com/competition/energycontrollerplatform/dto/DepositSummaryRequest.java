package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 充值统计查询请求。
 */
@Data
@Schema(description = "充值统计查询请求")
public class DepositSummaryRequest {

    @Schema(description = "统计粒度：DAY-按日, MONTH-按月, YEAR-按年", example = "MONTH",
            allowableValues = {"DAY", "MONTH", "YEAR"})
    private String granularity = "MONTH";

    @Schema(description = "开始日期", example = "2026-01-01")
    private String startDate;

    @Schema(description = "结束日期", example = "2026-05-31")
    private String endDate;

    @Schema(description = "商户ID（ADMIN可指定，非ADMIN强制覆盖）")
    private Long customerId;
}
