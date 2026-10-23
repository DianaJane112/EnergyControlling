package com.competition.energycontrollerplatform.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 哈希生成工具（命令行运行）。
 * <p>
 * 用于无法通过 DataInitializer 环境变量方式创建用户时（如 SQL 直接部署），
 * 手动生成 BCrypt 哈希值，填入 SQL 脚本中。
 * <p>
 * 使用方式：
 * <pre>
 *   java -cp energy-platform.jar com.competition.energycontrollerplatform.util.GenerateBcryptHash MyPassword
 * </pre>
 * 或在 Maven 项目中：
 * <pre>
 *   mvn exec:java -Dexec.mainClass="com.competition.energycontrollerplatform.util.GenerateBcryptHash" -Dexec.args="MyPassword"
 * </pre>
 */
public class GenerateBcryptHash {

    public static void main(String[] args) {
        if (args.length < 1 || args[0] == null || args[0].isBlank()) {
            System.err.println("用法: java GenerateBcryptHash <明文密码>");
            System.err.println("示例: java -cp app.jar com.competition.energycontrollerplatform.util.GenerateBcryptHash MyP@ss123");
            System.exit(1);
        }

        String plainPassword = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(plainPassword);

        System.out.println("══════════════════════════════════════════");
        System.out.println("  明文密码: " + plainPassword);
        System.out.println("  BCrypt 哈希: " + hash);
        System.out.println("══════════════════════════════════════════");
        System.out.println();
        System.out.println("SQL 示例:");
        System.out.println("INSERT INTO t_user (username, password, real_name, phone, role, status)");
        System.out.println("VALUES ('admin', '" + hash + "', '系统管理员', '13800000000', 'ADMIN', 1);");
    }
}
