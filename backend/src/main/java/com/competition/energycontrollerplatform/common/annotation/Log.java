package com.competition.energycontrollerplatform.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解。
 * <p>
 * 标注在 Controller 方法上，由 {@code LogAspect} 切面自动拦截并持久化操作记录。
 *
 * <pre>{@code
 * @Log(title = "商户管理", businessType = BusinessType.UPDATE)
 * @PutMapping("/update")
 * public Result<CustomerResponse> update(...) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 操作模块名称（如"商户管理"、"表计管理"、"资金计费"）
     */
    String title() default "";

    /**
     * 业务操作类型
     */
    BusinessType businessType() default BusinessType.OTHER;
}
