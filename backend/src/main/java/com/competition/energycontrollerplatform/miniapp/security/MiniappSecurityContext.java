package com.competition.energycontrollerplatform.miniapp.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 小程序专用 SecurityContext 工具类。
 * <p>
 * 从 Spring SecurityContext 中提取当前小程序用户的认证信息。
 * 业务代码通过此工具类获取 userId 和 customerId，实现数据隔离。
 */
public final class MiniappSecurityContext {

    private MiniappSecurityContext() {
        // 工具类，禁止实例化
    }

    /**
     * 获取当前小程序用户ID。
     *
     * @return 小程序用户ID，未认证时返回 null
     */
    public static Long getCurrentUserId() {
        MiniAppUserPrincipal principal = getPrincipal();
        return principal != null ? principal.getUserId() : null;
    }

    /**
     * 获取当前绑定的商户ID（用于数据隔离）。
     *
     * @return 商户ID，未认证时返回 null
     */
    public static Long getCurrentCustomerId() {
        MiniAppUserPrincipal principal = getPrincipal();
        return principal != null ? principal.getCustomerId() : null;
    }

    /**
     * 获取当前登录方式。
     *
     * @return WECHAT / PHONE，未认证时返回 null
     */
    public static String getCurrentLoginType() {
        MiniAppUserPrincipal principal = getPrincipal();
        return principal != null ? principal.getLoginType() : null;
    }

    /**
     * 是否已认证。
     */
    public static boolean isAuthenticated() {
        return getPrincipal() != null;
    }

    /**
     * 从 SecurityContext 中提取 MiniAppUserPrincipal。
     */
    private static MiniAppUserPrincipal getPrincipal() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof MiniAppUserPrincipal) {
            return (MiniAppUserPrincipal) principal;
        }
        return null;
    }
}
