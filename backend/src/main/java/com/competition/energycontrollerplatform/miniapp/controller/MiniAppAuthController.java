package com.competition.energycontrollerplatform.miniapp.controller;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.miniapp.dto.MiniAppLoginVO;
import com.competition.energycontrollerplatform.miniapp.dto.WechatLoginRequest;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;
import com.competition.energycontrollerplatform.miniapp.security.MiniappJwtUtils;
import com.competition.energycontrollerplatform.miniapp.security.MiniappSecurityContext;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序 - 登录认证控制器
 */
@RestController
@RequestMapping("/api/v1/miniapp/auth")
@RequiredArgsConstructor
@Tag(name = "小程序-登录认证", description = "微信登录、用户信息")
public class MiniAppAuthController {

    private final MiniAppUserService miniAppUserService;
    private final MiniappJwtUtils miniappJwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @PostMapping("/wechat-login")
    @Operation(summary = "微信登录", description = "使用微信 code 登录。账号需由管理员预先开通并绑定商户。")
    public Result<MiniAppLoginVO> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        MiniAppLoginVO vo = miniAppUserService.wechatLogin(request);
        return Result.success("登录成功", vo);
    }

    @PostMapping("/bind-customer")
    @Operation(summary = "绑定商户（暂未开放）", description = "当前版本采用管理员预开通账号，不开放小程序端自助绑定。")
    public Result<Void> bindCustomer() {
        throw BusinessException.of(403, "自助绑定暂未开放，请联系管理员开通账号");
    }

    @PostMapping("/phone-login")
    @Operation(summary = "手机号登录（暂未开放）", description = "当前版本仅保留微信登录。")
    public Result<Void> phoneLogin() {
        throw BusinessException.of(403, "手机号登录暂未开放，请使用微信登录");
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "返回当前小程序用户信息、商户信息和表计列表")
    public Result<MiniAppLoginVO> me() {
        Long userId = MiniappSecurityContext.getCurrentUserId();
        MiniAppLoginVO vo = miniAppUserService.getCurrentUserInfo(userId);
        return Result.success(vo);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用当前有效 Token 换取新的 Token（延长有效期），tokenVersion 不变")
    public Result<MiniAppLoginVO> refresh() {
        Long userId = MiniappSecurityContext.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(Result.CODE_UNAUTHORIZED, "未登录或 Token 已过期");
        }
        MiniAppLoginVO vo = miniAppUserService.getCurrentUserInfo(userId);
        return Result.success("Token 刷新成功", vo);
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "废止当前用户的所有 Token（递增 tokenVersion），客户端需删除本地 Token")
    public Result<String> logout() {
        Long userId = MiniappSecurityContext.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(Result.CODE_UNAUTHORIZED, "未登录");
        }
        miniAppUserService.incrementTokenVersion(userId);
        stringRedisTemplate.delete("token_version:miniapp_user:" + userId);
        return Result.success("已登出", "OK");
    }
}
