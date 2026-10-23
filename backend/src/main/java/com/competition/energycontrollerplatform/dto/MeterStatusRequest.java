package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 表计状态变更请求
 */
@Data
@Schema(description = "表计状态变更请求")
public class MeterStatusRequest {

    @Schema(description = "运行状态：0-停用，1-正常运行，2-欠费停机", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值范围 0~2")
    @Max(value = 2, message = "状态值范围 0~2")
    private Integer status;
}
