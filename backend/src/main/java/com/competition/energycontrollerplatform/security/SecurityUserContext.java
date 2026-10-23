package com.competition.energycontrollerplatform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文静态工具类。
 * <p>
 * 业务层通过此类直接获取当前登录用户的信息，
 * 无需在方法参数中层层传递。
 *
 * <pre>{@code
 * Long currentUserId = SecurityUserContext.getCurrentUserId();
 * String role = SecurityUserContext.getCurrentUserRole();
 * }</pre>
 */
public final class SecurityUserContext {

    private SecurityUserContext() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前登录用户的 ID。
     *
     * @return 用户 ID，如果未认证则返回 null
     */
    public static Long getCurrentUserId() {
        JwtUserPrincipal principal = getPrincipal();
        return principal != null ? principal.getUserId() : null;
    }

    /**
     * 获取当前登录用户的用户名。
     *
     * @return 用户名，如果未认证则返回 null
     */
    public static String getCurrentUsername() {
        JwtUserPrincipal principal = getPrincipal();
        return principal != null ? principal.getUsername() : null;
    }

    /**
     * 获取当前登录用户的角色编码。
     *
     * @return 角色（如 ADMIN、OPERATOR），如果未认证则返回 null
     */
    public static String getCurrentUserRole() {
        JwtUserPrincipal principal = getPrincipal();
        return principal != null ? principal.getRole() : null;
    }

    /**
     * 判断当前用户是否为管理员。
     */
    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getCurrentUserRole());
    }

    /**
     * 获取当前登录用户关联的商户 ID。
     * <p>
     * 当前架构下 userId 即映射为 customerId；
     * 后续可改为从 {@code t_user.customer_id} 字段读取。
     *
     * @return 商户 ID，如果未认证则返回 null
     */
    public static Long getCurrentCustomerId() {
        JwtUserPrincipal principal = getPrincipal();
        return principal != null ? principal.getUserId() : null;
    }

    /**
     * 从 SecurityContext 中提取 JWT 用户主体。
     */
    private static JwtUserPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUserPrincipal) {
            return (JwtUserPrincipal) authentication.getPrincipal();
        }
        return null;
    }
}
