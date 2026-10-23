package com.competition.energycontrollerplatform.common.annotation;

import java.lang.annotation.*;

/**
 * 敏感字段标记注解。
 * 标注在 DTO 字段上，{@link com.competition.energycontrollerplatform.common.aspect.LogAspect}
 * 在序列化操作日志时会自动对标注字段进行脱敏处理。
 * <p>
 * 脱敏由 {@link com.competition.energycontrollerplatform.common.SensitiveDataMasker} 执行。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sensitive {

    /** 脱敏类型 */
    SensitiveType value() default SensitiveType.PASSWORD;

    enum SensitiveType {
        /** 密码/密钥 → 替换为 [FILTERED] */
        PASSWORD,
        /** 手机号 → 138****1234 */
        PHONE,
        /** 验证码 → 替换为 [FILTERED] */
        CODE,
        /** OpenID → ox****AB */
        OPENID
    }
}
