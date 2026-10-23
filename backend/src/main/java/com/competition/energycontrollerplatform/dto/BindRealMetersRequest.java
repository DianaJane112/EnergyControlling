package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 虚拟电表关联真实电表请求 DTO。
 */
@Data
@Schema(description = "虚拟电表关联真实电表请求")
public class BindRealMetersRequest {

    @Schema(description = "要关联的真实电表ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少选择一台电表")
    private List<Long> meterIds;
}
