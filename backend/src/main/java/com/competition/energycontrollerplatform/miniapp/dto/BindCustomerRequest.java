package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信用户绑定商户请求。
 * <p>
 * 微信登录后，新用户通过手机号验证码绑定已有商户账户。
 */
@Data
@Schema(description = "微信用户绑定商户请求")
public class BindCustomerRequest {

    @Schema(description = "微信 openid（由 wx.login 获取 code 后调用 wechat-login 获得）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "openid 不能为空")
    private String openid;

    @Schema(description = "微信 unionid（可选，用于跨应用用户识别：公众号与小程序的用户打通）")
    private String unionid;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Schema(description = "短信验证码（开发模式固定 000000）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "短信验证码不能为空")
    private String code;
}
