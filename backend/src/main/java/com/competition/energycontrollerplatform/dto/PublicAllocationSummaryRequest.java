package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公共分摊统计查询请求。
 * <p>公共分摊为公区费用，不涉及单商户筛选，因此无需 customerId 字段。</p>
 */
@Data
@Schema(description = "公共分摊统计查询请求")
public class PublicAllocationSummaryRequest {

    @Schema(description = "统计粒度：DAY-按日, MONTH-按月, YEAR-按年", example = "MONTH",
            allowableValues = {"DAY", "MONTH", "YEAR"})
    private String granularity = "MONTH";

    @Schema(description = "开始日期", example = "2026-01-01")
    private String startDate;

    @Schema(description = "结束日期", example = "2026-05-31")
    private String endDate;

    @Schema(description = "分摊方式：PER_HOUSEHOLD-按户等分, BY_USAGE_RATIO-按历史用量比例分摊")
    private String allocationType;
}
