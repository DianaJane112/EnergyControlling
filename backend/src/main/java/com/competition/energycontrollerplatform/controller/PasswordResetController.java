package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.AdminResetPasswordRequest;
import com.competition.energycontrollerplatform.dto.ResetPasswordRequest;
import com.competition.energycontrollerplatform.dto.SendResetCodeRequest;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 密码重置 Controller。
 * <p>
 * 提供：
 * <ul>
 *   <li>自助重置 — 短信验证码方式（无需登录）</li>
 *   <li>管理员后台重置 — ADMIN 角色为用户重置密码（需登录）</li>
 * </ul>
 */
@Tag(name = "系统 - 密码重置", description = "忘记密码自助重置 + 管理员后台重置")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // ──────────────── 自助重置（无需登录） ────────────────

    @Operation(summary = "发送重置密码验证码",
            description = "向指定用户的绑定手机号发送短信验证码。"
                    + "同一用户名60秒内只能发送一次，同一IP每分钟最多3次。"
                    + "验证码5分钟有效，最多尝试3次。")
    @PostMapping("/send-reset-code")
    public Result<String> sendResetCode(@Valid @RequestBody SendResetCodeRequest request,
                                        HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        passwordResetService.sendResetCode(
                request.getUsername(), request.getPhone(), clientIp);
        return Result.success("验证码已发送，5分钟内有效", "");
    }

    @Operation(summary = "验证码重置密码",
            description = "通过短信验证码重置密码。验证码一次性使用，最多尝试3次。"
                    + "重置成功后请使用新密码重新登录。")
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(
                request.getUsername(), request.getCode(), request.getNewPassword());
        return Result.success("密码重置成功，请使用新密码登录", "");
    }

    // ──────────────── 管理员后台重置（需登录，仅 ADMIN） ────────────────

    @Operation(summary = "管理员后台重置用户密码",
            description = "ADMIN 角色为指定用户重置密码，无需验证码。仅 ADMIN 可执行。")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PostMapping("/admin-reset-password")
    public Result<String> adminResetPassword(@Valid @RequestBody AdminResetPasswordRequest request) {
        // 权限校验：仅 ADMIN 可执行
        if (!SecurityUserContext.isAdmin()) {
            throw BusinessException.of(403, "仅管理员可执行此操作");
        }

        String adminName = SecurityUserContext.getCurrentUsername();
        passwordResetService.adminResetPassword(
                request.getUserId(), request.getNewPassword(), adminName);
        return Result.success("密码已重置", "");
    }

    // ──────────────── 私有方法 ────────────────

    /**
     * 从请求中提取客户端真实 IP。
     * <p>
     * 优先读取反向代理/负载均衡传递的 X-Forwarded-For / X-Real-IP 头，
     * 兼容 Nginx、Cloudflare、阿里云 SLB 等常见代理方案。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个IP，取第一个（客户端真实IP）
            int commaIdx = ip.indexOf(',');
            if (commaIdx > 0) {
                ip = ip.substring(0, commaIdx);
            }
            return ip.trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }
}
