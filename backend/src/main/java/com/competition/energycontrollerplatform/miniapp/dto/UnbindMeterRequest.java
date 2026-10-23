package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 小程序解绑表计请求。
 */
@Data
@Schema(description = "解绑表计请求")
public class UnbindMeterRequest {

    @NotNull(message = "表计ID不能为空")
    @Schema(description = "要解绑的表计ID", example = "42")
    private Long meterId;
}
