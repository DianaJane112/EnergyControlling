package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录成功响应
 */
@Getter
@AllArgsConstructor
@Schema(description = "登录成功响应")
public class LoginVO {

    @Schema(description = "JWT 访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private final String token;

    @Schema(description = "用户名", example = "admin")
    private final String username;

    @Schema(description = "角色编码", example = "ADMIN")
    private final String role;

    @Schema(description = "真实姓名", example = "系统管理员")
    private final String realName;
}
