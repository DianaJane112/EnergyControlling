package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Water meter vendor integration configuration request.
 */
@Data
@Schema(description = "水表厂商对接配置请求")
public class WaterMeterVendorRequest {

    @NotBlank(message = "水表厂商名称不能为空")
    @Schema(description = "水表厂商名称", example = "青岛积成电子", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vendorName;

    @NotBlank(message = "厂商编码不能为空")
    @Schema(description = "厂商编码", example = "JICHENG", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vendorCode;

    @NotBlank(message = "协议类型不能为空")
    @Schema(description = "协议类型", example = "JICHENG_HTTP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String protocolType;

    @Schema(description = "API地址", example = "https://api.example.com")
    private String apiUrl;

    @Schema(description = "认证方式", example = "SIGN")
    private String authType;

    @Schema(description = "AppId", example = "your_app_id")
    private String appId;

    @Schema(description = "AppSecret", example = "your_app_secret")
    private String appSecret;

    @Schema(description = "Webhook签名密钥", example = "webhook_secret")
    private String webhookSignature;

    @Schema(description = "厂商扩展配置JSON", example = "{\"sign\":\"xxx\",\"companyCode\":\"0001\"}")
    private String configJson;

    @Schema(description = "状态：0-停用, 1-启用", example = "1")
    private Integer status;
}
