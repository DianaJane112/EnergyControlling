package com.competition.energycontrollerplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 当前登录用户信息响应。
 * <p>
 * 用于 /api/v1/auth/me 接口，返回从 SecurityContext + 数据库提取的用户信息。
 * 与 {@link LoginVO} 的区别：不含 token 字段。
 */
@Getter
@AllArgsConstructor
@Schema(description = "当前用户信息")
public class UserInfoVO {

    @Schema(description = "用户名", example = "admin")
    private final String username;

    @Schema(description = "角色编码", example = "ADMIN")
    private final String role;

    @Schema(description = "真实姓名", example = "系统管理员")
    private final String realName;
}
