package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 余额清零请求 DTO。
 * <p>
 * 清零前将记录清零前余额→资金流水，同时更新表计状态位。
 */
@Data
@Schema(description = "余额清零请求")
public class ClearBalanceRequest {

    @Schema(description = "表计ID（由路径参数自动注入，无需手动传入）", example = "1")
    // ⚠️ meterId 由 Controller 从路径 @PathVariable 注入，此处不加 @NotNull
    // 否则 @Valid 会在 setMeterId() 之前校验，导致误报 400
    private Long meterId;

    @Schema(description = "清零原因（必填，审计用）", example = "商户退租，退还全部余额后清零", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "清零原因不能为空")
    private String reason;
}
