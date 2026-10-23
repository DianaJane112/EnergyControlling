package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 基站绑定/解绑电表/水表请求 DTO。
 */
@Data
@Schema(description = "基站绑定电表/水表请求")
public class CollectorBindMeterRequest {

    @Schema(description = "表计ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "表计ID不能为空")
    private Long meterId;
}
