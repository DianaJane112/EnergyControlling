package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 扫码查询表计请求
 */
@Data
@Schema(description = "扫码查询表计请求")
public class ScanMeterRequest {

    @Schema(description = "二维码内容（纯表号 或 METER:{表号}）", example = "010000000001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "二维码内容不能为空")
    private String qrContent;
}
