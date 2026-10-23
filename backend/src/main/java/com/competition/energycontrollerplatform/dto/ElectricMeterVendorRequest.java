package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 电表厂商对接配置新增/编辑请求。
 */
@Data
@Schema(description = "电表厂商对接配置请求")
public class ElectricMeterVendorRequest {

    @NotBlank(message = "电表厂商名称不能为空")
    @Schema(description = "电表厂商名称", example = "辰鳗电表", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vendorName;

    @NotBlank(message = "API地址不能为空")
    @Schema(description = "API地址", example = "https://api.smalleel.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiUrl;

    @NotBlank(message = "AppId不能为空")
    @Schema(description = "AppId", example = "your_app_id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appId;

    @NotBlank(message = "AppSecret不能为空")
    @Schema(description = "AppSecret", example = "your_app_secret", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appSecret;

    @Schema(description = "Webhook签名密钥", example = "haopu_webhook_secret")
    private String webhookSignature;

    @Schema(description = "状态：0-停用, 1-启用", example = "1")
    private Integer status;
}
