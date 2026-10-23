package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 表计读数分页查询请求
 */
@Data
@Schema(description = "表计读数分页查询请求")
public class MeterReadingPageRequest {

    @Schema(description = "页码（从1开始）", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "20")
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 20;

    @Schema(description = "关联表计ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long meterId;

    @Schema(description = "开始时间戳（毫秒）", example = "1735564800000")
    private Long startTime;

    @Schema(description = "结束时间戳（毫秒）", example = "1735651200000")
    private Long endTime;
}
