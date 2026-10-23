package com.competition.energycontrollerplatform.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.entity.Role;
import com.competition.energycontrollerplatform.entity.User;
import com.competition.energycontrollerplatform.mapper.RoleMapper;
import com.competition.energycontrollerplatform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 首次启动数据初始化。
 * <p>
 * 自动创建默认角色和管理员 / 运营人员账号（如不存在）。
 * <p>
 * 要求通过环境变量或配置文件设置初始密码：
 * <ul>
 *   <li>{@code ADMIN_INITIAL_PASSWORD} — 管理员初始密码（必填，dev 在 application.yml 中预设固定值）</li>
 *   <li>{@code OPERATOR_INITIAL_PASSWORD} — 运营人员初始密码（必填，dev 在 application.yml 中预设固定值）</li>
 * </ul>
 * 密码未配置时拒绝创建账号并记录 ERROR 日志。
 * 账号已存在时不会覆盖已有密码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.initial-password:}")
    private String adminInitialPassword;

    @Value("${operator.initial-password:}")
    private String operatorInitialPassword;

    @Override
    public void run(String... args) {
        initRoles();
        initUsers();
    }

    private void initRoles() {
        if (roleMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
            log.info("角色数据已存在，跳过初始化");
            return;
        }

        insertRole("系统管理员", "ADMIN",
                "[\"meter:view\",\"meter:edit\",\"meter:delete\",\"customer:view\",\"customer:edit\",\"customer:delete\",\"report:view\",\"report:export\",\"user:manage\",\"role:manage\",\"system:config\"]");

        insertRole("运营人员", "OPERATOR",
                "[\"meter:view\",\"meter:edit\",\"customer:view\",\"customer:edit\",\"report:view\",\"report:export\"]");

        insertRole("审计员", "AUDITOR",
                "[\"meter:view\",\"customer:view\",\"report:view\",\"report:export\",\"log:view\"]");

        log.info("✅ 默认角色初始化完成");
    }

    private void insertRole(String roleName, String roleCode, String permissions) {
        Role role = new Role();
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setPermissions(permissions);
        role.setStatus(1);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
    }

    // ──────────────── 用户初始化 ────────────────

    private void initUsers() {
        // ── 管理员 admin ──
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")) == 0) {
            String pwd = resolvePassword(adminInitialPassword, "admin");
            if (pwd != null) {
                insertUser("admin", pwd, "系统管理员", "13800000000", "ADMIN");
                log.info("✅ 默认管理员 admin 已创建");
            }
        } else {
            log.info("管理员账号已存在，跳过初始化");
        }

        // ── 运营人员 operator ──
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "operator")) == 0) {
            String pwd = resolvePassword(operatorInitialPassword, "operator");
            if (pwd != null) {
                insertUser("operator", pwd, "运营小王", "13800000001", "OPERATOR");
                log.info("✅ 默认运营账号 operator 已创建");
            }
        } else {
            log.info("运营账号已存在，跳过初始化");
        }
    }

    /**
     * 解析初始密码（profile 无关）。
     * <p>
     * 环境变量或配置文件未设置密码时拒绝创建账号并记录 ERROR 日志，
     * 杜绝任何环境使用硬编码默认密码。
     * <p>
     * 开发环境请在 {@code application.yml} 的 {@code admin.initial-password}
     * 和 {@code operator.initial-password} 中设置固定值。
     *
     * @param rawValue 环境变量 / 配置文件原始值（可为空）
     * @param username 用户名（用于日志）
     * @return 明文密码，或 null 表示跳过创建
     */
    private String resolvePassword(String rawValue, String username) {
        if (rawValue != null && !rawValue.isBlank()) {
            return rawValue.trim();
        }
        String envVar = "admin".equals(username) ? "ADMIN_INITIAL_PASSWORD" : "OPERATOR_INITIAL_PASSWORD";
        log.error("❌ 未配置 {} 环境变量，跳过 {} 账号创建。" +
                  "请设置该环境变量或配置文件后重启应用。", envVar, username);
        return null;
    }

    private void insertUser(String username, String rawPassword, String realName, String phone, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }
}
