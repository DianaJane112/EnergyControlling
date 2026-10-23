package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 公共分摊分页查询请求。
 */
@Data
@Schema(description = "公共分摊分页查询请求")
public class PublicAllocationPageRequest {

    @Schema(description = "页码（从1开始）", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10;

    @Schema(description = "分摊名称（模糊查询）", example = "2026年5月")
    private String allocationName;

    @Schema(description = "分摊方式：PER_HOUSEHOLD-按户等分, BY_USAGE_RATIO-按历史用量比例分摊")
    private String allocationType;

    @Schema(description = "状态：0-待执行，1-已执行，2-已撤销")
    private Integer status;

    @Schema(description = "生效日期起（含）", example = "2026-05-01")
    private String startDate;

    @Schema(description = "生效日期止（含）", example = "2026-05-31")
    private String endDate;
}
