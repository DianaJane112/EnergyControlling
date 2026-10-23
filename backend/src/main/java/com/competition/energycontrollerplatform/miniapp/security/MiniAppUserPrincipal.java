package com.competition.energycontrollerplatform.miniapp.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 小程序用户认证主体。
 * <p>
 * 在 JWT 认证通过后存入 SecurityContext 的 Principal，
 * 后续业务代码通过 SecurityContext 获取当前小程序用户信息。
 */
@Getter
@AllArgsConstructor
public class MiniAppUserPrincipal {

    /** 小程序用户ID（t_miniapp_user.id） */
    private final Long userId;

    /** 绑定的商户ID（t_customer.id） */
    private final Long customerId;

    /** 登录方式: WECHAT / PHONE */
    private final String loginType;
}
