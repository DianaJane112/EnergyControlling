package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 小程序充值提交请求
 */
@Data
@Schema(description = "小程序充值提交请求")
public class RechargeSubmitRequest {

    @Schema(description = "表计ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "表计ID不能为空")
    private Long meterId;

    @Schema(description = "充值金额（元），必须大于0", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "支付方式（固定为 VIRTUAL）", example = "VIRTUAL")
    private String payMethod = "VIRTUAL";
}
