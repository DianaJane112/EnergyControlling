package com.competition.energycontrollerplatform.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * JWT 认证成功后存储在 SecurityContext 中的用户主体。
 * <p>
 * 封装了当前登录用户的 ID、用户名和角色，
 * 业务层通过 {@link SecurityUserContext} 读取。
 */
@Getter
@AllArgsConstructor
public class JwtUserPrincipal {

    /** 用户主键 ID */
    private final Long userId;

    /** 用户名 */
    private final String username;

    /** 角色编码（ADMIN / OPERATOR / MERCHANT） */
    private final String role;
}
