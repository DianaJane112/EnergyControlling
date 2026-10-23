package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 调账请求（运维手动修正余额）
 */
@Data
@Schema(description = "余额调整请求")
public class AdjustBalanceRequest {

    @Schema(description = "商户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商户ID不能为空")
    private Long customerId;

    @Schema(description = "调整金额（元），正数=增加余额，负数=扣减余额，不能为0", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "调整金额不能为空")
    private BigDecimal amount;

    @Schema(description = "操作人", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "操作人不能为空")
    private String operatorName;

    @Schema(description = "调账原因（必填，用于审计）", example = "上月电费少扣，补扣差额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "调账原因不能为空")
    private String reason;

    @Schema(description = "备注")
    private String remark;
}
