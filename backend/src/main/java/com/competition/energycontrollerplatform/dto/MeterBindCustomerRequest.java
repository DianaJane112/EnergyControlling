package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 表计绑定商户请求
 */
@Data
@Schema(description = "表计绑定商户请求")
public class MeterBindCustomerRequest {

    @Schema(description = "商户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商户ID不能为空")
    private Long customerId;
}
