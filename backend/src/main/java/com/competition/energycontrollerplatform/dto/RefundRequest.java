package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退费请求
 */
@Data
@Schema(description = "退费请求")
public class RefundRequest {

    @Schema(description = "商户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商户ID不能为空")
    private Long customerId;

    @Schema(description = "退费金额（元），必须大于0", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退费金额不能为空")
    @DecimalMin(value = "0.01", message = "退费金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "操作人", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "操作人不能为空")
    private String operatorName;

    @Schema(description = "退费原因", example = "商户退租，退还剩余电费")
    private String reason;

    @Schema(description = "备注")
    private String remark;
}
