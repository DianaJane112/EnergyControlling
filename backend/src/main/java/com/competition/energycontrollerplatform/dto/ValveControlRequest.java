package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 远程分合闸控制请求 DTO。
 */
@Data
@Schema(description = "远程分合闸请求")
public class ValveControlRequest {

    @NotBlank(message = "操作原因不能为空")
    @Schema(description = "操作原因", example = "欠费停机")
    private String reason;
}
