package com.competition.energycontrollerplatform.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "小程序登录响应")
public class MiniAppLoginVO {

    @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "微信 openid（新用户绑定商户时需要回传）")
    private String openid;

    @Schema(description = "微信 unionid（跨应用用户唯一标识，绑定商户时回传）")
    private String unionid;

    @Schema(description = "是否需要绑定商户（微信新用户首次登录时为 true）")
    private Boolean needBindCustomer;

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    @Schema(description = "商户信息")
    private CustomerInfo customerInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "小程序用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "昵称")
        private String nickname;
        @Schema(description = "头像URL")
        private String avatarUrl;
        @Schema(description = "手机号")
        private String phone;
        @Schema(description = "登录方式")
        private String loginType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "商户信息")
    public static class CustomerInfo {
        @Schema(description = "商户ID")
        private Long customerId;
        @Schema(description = "商户名称")
        private String customerName;
        @Schema(description = "商户编号")
        private String customerCode;
        @Schema(description = "当前余额")
        private java.math.BigDecimal balance;
    }
}
