package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.RateLimiter;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.ChangePasswordDTO;
import com.competition.energycontrollerplatform.dto.LoginDTO;
import com.competition.energycontrollerplatform.dto.LoginVO;
import com.competition.energycontrollerplatform.dto.UserInfoVO;
import com.competition.energycontrollerplatform.entity.User;
import com.competition.energycontrollerplatform.security.JwtUtils;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制层。
 * <p>
 * 提供登录接口，返回 JWT Token。
 * 该接口在 SecurityConfig 中已放行，无需认证即可访问。
 */
@Tag(name = "系统 - 认证", description = "登录、登出、获取当前用户信息等接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final RateLimiter rateLimiter;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATE_KEY_PREFIX = "rate:login:ip:";
    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final long LOGIN_WINDOW_SEC = 60L;

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，成功返回 JWT Token。同一IP每分钟最多5次")
    @Log(title = "用户认证", businessType = BusinessType.LOGIN)
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        // 0. 登录频率限制（防暴力破解）
        String clientIp = getClientIp(request);
        if (!rateLimiter.tryAcquire(RATE_KEY_PREFIX + clientIp, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW_SEC)) {
            return Result.error(429, "登录尝试过于频繁，请1分钟后再试");
        }
        // 1. 校验用户凭据
        User user = userService.authenticate(dto.getUsername(), dto.getPassword());

        // 2. 生成 JWT Token（含 tokenVersion，修改密码后旧 Token 即时报废）
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());

        // 3. 更新最后登录时间
        userService.updateLastLoginTime(user.getId());

        // 4. 组装响应
        LoginVO vo = new LoginVO(token, user.getUsername(), user.getRole(), user.getRealName());
        return Result.success("登录成功", vo);
    }

    @Operation(summary = "获取当前用户信息", description = "验证登录态是否有效，返回当前登录用户的用户名、角色、真实姓名（需要携带 Token）")
    @PostMapping("/me")
    public Result<UserInfoVO> me() {
        // 从 SecurityContext 安全提取当前登录用户 ID（杜绝水平越权）
        Long userId = SecurityUserContext.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(Result.CODE_UNAUTHORIZED, "未登录或 Token 已过期");
        }
        User user = userService.getById(userId);
        UserInfoVO vo = new UserInfoVO(user.getUsername(), user.getRole(), user.getRealName());
        return Result.success(vo);
    }

    @Operation(summary = "修改密码", description = "修改当前登录用户的密码。修改后旧 Token 全部失效，需重新登录。")
    @Log(title = "用户认证", businessType = BusinessType.UPDATE)
    @PostMapping("/change-password")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = SecurityUserContext.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(Result.CODE_UNAUTHORIZED, "未登录或 Token 已过期");
        }
        userService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        // 修改密码后递增 tokenVersion → 所有旧 Token 即时报废
        userService.incrementTokenVersion(userId);
        stringRedisTemplate.delete("token_version:user:" + userId);
        return Result.success("密码修改成功，请重新登录", "OK");
    }

    @Operation(summary = "刷新Token", description = "使用当前有效 Token 换取新的 Token（延长有效期），tokenVersion 不变。")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh() {
        Long userId = SecurityUserContext.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(Result.CODE_UNAUTHORIZED, "未登录或 Token 已过期");
        }
        User user = userService.getById(userId);
        String newToken = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
        LoginVO vo = new LoginVO(newToken, user.getUsername(), user.getRole(), user.getRealName());
        return Result.success("Token 刷新成功", vo);
    }

    @Operation(summary = "登出", description = "废止当前用户的所有 Token（递增 tokenVersion），客户端需删除本地存储的 Token。")
    @PostMapping("/logout")
    public Result<String> logout() {
        Long userId = SecurityUserContext.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(Result.CODE_UNAUTHORIZED, "未登录");
        }
        userService.incrementTokenVersion(userId);
        stringRedisTemplate.delete("token_version:user:" + userId);
        return Result.success("已登出，所有设备上的 Token 均已失效", "OK");
    }

    /**
     * 从请求中提取客户端真实 IP。
     * 优先读取反向代理/负载均衡传递的 X-Forwarded-For / X-Real-IP 头。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
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
