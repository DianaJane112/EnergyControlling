package com.competition.energycontrollerplatform.miniapp.dto;

import com.competition.energycontrollerplatform.common.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手机号登录请求
 */
@Data
@Schema(description = "手机号登录请求")
public class PhoneLoginRequest {

    @Sensitive(Sensitive.SensitiveType.PHONE)
    @Schema(description = "手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Sensitive(Sensitive.SensitiveType.CODE)
    @Schema(description = "短信验证码（开发阶段固定为 000000）", example = "000000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证码不能为空")
    private String code;
}
