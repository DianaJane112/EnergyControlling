package com.competition.energycontrollerplatform.common;

/**
 * 敏感数据脱敏工具类。
 * <p>
 * 所有日志输出敏感字段前必须通过此类处理。
 * 提供手机号、验证码、OpenID、密码的标准化脱敏方法。
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}

    /**
     * 手机号脱敏：13812341234 → 138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "[FILTERED]";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 验证码脱敏：永远不记录验证码 → 返回 [FILTERED]
     */
    public static String maskCode(String code) {
        return "[FILTERED]";
    }

    /**
     * OpenID 脱敏：oxABCD1234 → ox****AB
     */
    public static String maskOpenId(String openid) {
        if (openid == null || openid.length() < 6) return "[FILTERED]";
        return openid.substring(0, 2) + "****" + openid.substring(openid.length() - 2);
    }

    /**
     * 密码脱敏：任何密码 → [FILTERED]
     */
    public static String maskPassword(String password) {
        return "[FILTERED]";
    }
}
