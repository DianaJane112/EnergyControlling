package com.competition.energycontrollerplatform.service;

/**
 * 密码重置服务接口。
 * <p>
 * 提供两种密码重置方式：
 * <ol>
 *   <li><b>自助重置</b> — 短信验证码验证后重置（无需登录）</li>
 *   <li><b>管理员重置</b> — ADMIN 角色在后台为用户重置密码</li>
 * </ol>
 */
public interface PasswordResetService {

    /**
     * 发送重置密码验证码。
     * <p>
     * 防刷策略：同一用户名60秒内只能发送一次，同一IP每分钟最多3次。
     *
     * @param username 用户名
     * @param phone    手机号（需与用户绑定的手机号一致）
     * @param clientIp 客户端IP
     */
    void sendResetCode(String username, String phone, String clientIp);

    /**
     * 通过验证码重置密码。
     * <p>
     * 验证码一次性使用（防重放），最多尝试3次。
     *
     * @param username    用户名
     * @param code        短信验证码
     * @param newPassword 新密码
     */
    void resetPassword(String username, String code, String newPassword);

    /**
     * 管理员后台重置用户密码。
     * <p>
     * 仅 ADMIN 可执行，调用方需在 Controller 层校验权限。
     *
     * @param targetUserId 目标用户ID
     * @param newPassword  新密码
     * @param adminName    操作管理员用户名（用于审计日志）
     */
    void adminResetPassword(Long targetUserId, String newPassword, String adminName);
}
