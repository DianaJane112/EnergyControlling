package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.RateLimiter;
import com.competition.energycontrollerplatform.common.SensitiveDataMasker;
import com.competition.energycontrollerplatform.entity.User;
import com.competition.energycontrollerplatform.mapper.UserMapper;
import com.competition.energycontrollerplatform.service.PasswordResetService;
import com.competition.energycontrollerplatform.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 密码重置服务实现。
 * <p>
 * 安全策略：
 * <ul>
 *   <li>验证码 6 位数字，5 分钟有效</li>
 *   <li>同一用户名 60 秒内只能发送一次</li>
 *   <li>同一 IP 每分钟最多 3 次发送请求</li>
 *   <li>验证码最多尝试 3 次，超出后失效</li>
 *   <li>验证码一次性使用（取即删），防止重放</li>
 *   <li>用户名 + 手机号双重校验</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private static final String REDIS_PREFIX_RESET_CODE = "reset:code:";
    private static final String REDIS_PREFIX_RESET_TRY = "reset:try:";
    private static final String REDIS_PREFIX_RESET_SEND = "reset:send:";
    private static final String REDIS_PREFIX_RATE_IP = "reset:rate:ip:";

    /** 验证码有效期（秒） */
    private static final long CODE_TIMEOUT_SEC = 300L; // 5 分钟
    /** 同用户名发送间隔（秒） */
    private static final long SEND_INTERVAL_SEC = 60L;
    /** 验证码最大尝试次数 */
    private static final int MAX_TRY_COUNT = 3;
    /** 同 IP 每窗口最大发送次数 */
    private static final int IP_RATE_LIMIT = 3;
    /** IP 限流窗口（秒） */
    private static final long IP_RATE_WINDOW_SEC = 60L;

    private final UserMapper userMapper;
    private final SmsService smsService;
    private final RateLimiter rateLimiter;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    /** 短信开发模式：true 时使用固定验证码 */
    @Value("${miniapp.sms.dev-mode:true}")
    private boolean smsDevMode;

    /** 开发模式固定验证码 */
    @Value("${miniapp.sms.dev-code:000000}")
    private String devCode;

    // ──────────────────────── 发送验证码 ────────────────────────

    @Override
    public void sendResetCode(String username, String phone, String clientIp) {
        // ① IP 级别频率限制
        String ipRateKey = REDIS_PREFIX_RATE_IP + clientIp;
        if (!rateLimiter.tryAcquire(ipRateKey, IP_RATE_LIMIT, IP_RATE_WINDOW_SEC)) {
            log.warn("[密码重置] IP限流触发 ip={}, username={}", clientIp, username);
            throw BusinessException.of(429, "操作过于频繁，请稍后再试");
        }

        // ② 根据用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getStatus, 1));
        if (user == null) {
            // 不暴露"用户不存在"以避免用户枚举攻击
            log.warn("[密码重置] 用户不存在或已禁用 username={}", username);
            throw BusinessException.of(400, "用户名与手机号不匹配");
        }

        // ③ 校验手机号
        if (!StringUtils.hasText(user.getPhone()) || !user.getPhone().equals(phone)) {
            log.warn("[密码重置] 手机号不匹配 username={}, dbPhone={}, reqPhone={}",
                    username, SensitiveDataMasker.maskPhone(user.getPhone()), SensitiveDataMasker.maskPhone(phone));
            throw BusinessException.of(400, "用户名与手机号不匹配");
        }

        // ④ 同用户名发送频率限制
        String sendKey = REDIS_PREFIX_RESET_SEND + username;
        if (!rateLimiter.tryAcquire(sendKey, 1, SEND_INTERVAL_SEC)) {
            long remaining = SEND_INTERVAL_SEC - rateLimiter.getCount(sendKey);
            throw BusinessException.of(429,
                    String.format("验证码已发送，请 %d 秒后再试", Math.max(remaining, 1)));
        }

        // ⑤ 生成验证码（开发模式使用固定验证码，生产环境随机生成）
        String code = smsDevMode ? devCode : generateCode();

        // ⑥ 存入 Redis（带过期）
        String codeKey = REDIS_PREFIX_RESET_CODE + username;
        rateLimiter.setCode(codeKey, code, CODE_TIMEOUT_SEC);

        // ⑦ 重置尝试次数
        String tryKey = REDIS_PREFIX_RESET_TRY + username;
        rateLimiter.reset(tryKey);

        // ⑧ 发送短信
        smsService.sendVerificationCode(phone, code, "重置密码");

        log.info("[密码重置] 验证码已发送 username={}, phone={}, ip={}", username, SensitiveDataMasker.maskPhone(phone), clientIp);
    }

    // ──────────────────────── 重置密码 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String username, String code, String newPassword) {
        // ① 检查尝试次数
        String tryKey = REDIS_PREFIX_RESET_TRY + username;
        if (!rateLimiter.tryAcquire(tryKey, MAX_TRY_COUNT, CODE_TIMEOUT_SEC)) {
            // 超出尝试次数，清除验证码
            String codeKey = REDIS_PREFIX_RESET_CODE + username;
            rateLimiter.reset(codeKey);
            log.warn("[密码重置] 验证码尝试次数超限 username={}", username);
            throw BusinessException.of(400, "验证码尝试次数过多，请重新获取验证码");
        }

        // ② 获取并删除（一次性使用）
        String codeKey = REDIS_PREFIX_RESET_CODE + username;
        String storedCode = rateLimiter.consumeCode(codeKey);

        if (storedCode == null) {
            throw BusinessException.of(400, "验证码已过期或未发送，请重新获取");
        }

        if (!storedCode.equals(code)) {
            long tries = rateLimiter.getCount(tryKey);
            long remaining = Math.max(0, MAX_TRY_COUNT - tries);
            throw BusinessException.of(400,
                    String.format("验证码错误，还剩 %d 次尝试机会", remaining));
        }

        // ③ 验证码正确，重置密码
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getStatus, 1));
        if (user == null) {
            throw BusinessException.of(400, "用户不存在或已禁用");
        }

        // BCrypt 加密新密码
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);

        // 清除尝试计数
        rateLimiter.reset(tryKey);

        log.warn("[密码重置] 密码已重置 username={}", username);
    }

    // ──────────────────────── 管理员重置 ────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminResetPassword(Long targetUserId, String newPassword, String adminName) {
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw BusinessException.of(400, "目标用户不存在");
        }

        // BCrypt 加密新密码
        User update = new User();
        update.setId(targetUserId);
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);

        log.warn("[管理员重置密码] targetUserId={} ({}) → admin={}",
                targetUserId, targetUser.getUsername(), adminName);
    }

    // ──────────────────────── 私有方法 ────────────────────────

    /**
     * 生成 6 位随机数字验证码。
     */
    private String generateCode() {
        int code = secureRandom.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(code);
    }
}
