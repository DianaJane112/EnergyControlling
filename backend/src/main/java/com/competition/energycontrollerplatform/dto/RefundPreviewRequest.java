package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退费预览请求。
 * <p>前端在退费弹窗中选中表计、填写退费金额后，
 * 调用此接口预览退费结果再确认提交。
 */
@Data
@Schema(description = "退费预览请求")
public class RefundPreviewRequest {

    @Schema(description = "表计ID（必填）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "表计ID不能为空")
    private Long meterId;

    @Schema(description = "拟退费金额（元），必须大于0", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退费金额不能为空")
    private BigDecimal refundAmount;

    @Schema(description = "调整后电价（元/kWh），可选，用于预览不同电价下的余额变化", example = "0.85")
    private BigDecimal adjustedPrice;
}
