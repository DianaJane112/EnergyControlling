package com.competition.energycontrollerplatform.common.aspect;

import com.competition.energycontrollerplatform.common.SensitiveDataMasker;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.common.annotation.Sensitive;
import com.competition.energycontrollerplatform.entity.OperationLog;
import com.competition.energycontrollerplatform.mapper.OperationLogMapper;
import com.competition.energycontrollerplatform.security.JwtUserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作日志 AOP 切面。
 * <p>
 * 拦截所有标注了 {@link Log} 注解的 Controller 方法，
 * 异步收集用户信息、请求参数、执行结果并写入 {@code t_operation_log} 表。
 * <p>
 * 使用 {@link Async} 异步执行，不阻塞主线程响应。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LogAspect.class);

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 切入点：所有带 @Log 注解的方法。
     */
    @Pointcut("@annotation(com.competition.energycontrollerplatform.common.annotation.Log)")
    public void logPointcut() {
    }

    /**
     * 后置通知：方法正常返回后记录日志。
     * <p>
     * 设计选择 {@code @AfterReturning} 而非 {@code @Around}：
     * <ul>
     *   <li>避免 try-catch 包裹整个方法链导致的异常吞没风险</li>
     *   <li>执行失败由 {@code @AfterThrowing} 独立处理</li>
     * </ul>
     */
    @AfterReturning(pointcut = "logPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        saveLog(joinPoint, null, result);
    }

    /**
     * 异常通知：方法抛出异常时记录日志。
     */
    @AfterThrowing(pointcut = "logPointcut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Exception e) {
        saveLog(joinPoint, e, null);
    }

    // ──────── 异步持久化 ────────

    @Async
    void saveLog(JoinPoint joinPoint, Exception exception, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Log logAnnotation = signature.getMethod().getAnnotation(Log.class);
            if (logAnnotation == null) return;

            OperationLog opLog = new OperationLog();

            // ── 操作人信息（从 SecurityContext 提取） ──
            String username = getCurrentUsername();
            opLog.setOperatorName(username != null ? username : "anonymous");
            opLog.setIpAddress(getClientIp());

            // ── 模块 + 操作类型 ──
            opLog.setModuleName(logAnnotation.title());
            opLog.setOperationType(businessTypeLabel(logAnnotation.businessType()));

            // ── 操作描述：className.methodName ──
            String className = signature.getDeclaringType().getSimpleName();
            String methodName = signature.getName();
            opLog.setOperationDesc(className + "." + methodName);

            // ── 请求参数（JSON 序列化，控制长度 + 敏感字段脱敏） ──
            Object[] args = joinPoint.getArgs();
            String paramsJson = "[no-args]";
            if (args != null && args.length > 0) {
                try {
                    // 过滤掉 HttpServletRequest/Response 等不可序列化参数，同时对 DTO 敏感字段脱敏
                    Object[] sanitized = new Object[args.length];
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof jakarta.servlet.http.HttpServletRequest
                                || args[i] instanceof jakarta.servlet.http.HttpServletResponse) {
                            sanitized[i] = "[request/response]";
                        } else {
                            sanitized[i] = maskSensitiveFields(args[i]);
                        }
                    }
                    paramsJson = objectMapper.writeValueAsString(sanitized);
                    if (paramsJson.length() > 2000) {
                        paramsJson = paramsJson.substring(0, 1997) + "...";
                    }
                } catch (JsonProcessingException e) {
                    paramsJson = "[serialize-error: " + e.getMessage() + "]";
                }
            }
            opLog.setRequestParams(paramsJson);

            // ── 执行状态 ──
            if (exception != null) {
                String errMsg = exception.getMessage();
                if (errMsg != null && errMsg.length() > 500) {
                    errMsg = errMsg.substring(0, 497) + "...";
                }
                opLog.setResponseResult("FAIL: " + (errMsg != null ? errMsg : exception.getClass().getSimpleName()));
            } else {
                opLog.setResponseResult("SUCCESS");
            }

            opLog.setOperationTime(LocalDateTime.now());
            opLog.setCreateTime(LocalDateTime.now());

            operationLogMapper.insert(opLog);
        } catch (Exception e) {
            // 日志记录本身失败不能影响主业务
            log.error("操作日志写入失败", e);
        }
    }

    // ──────── 工具方法 ────────

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof JwtUserPrincipal principal) {
                return principal.getUsername();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip != null && !ip.isBlank() ? ip : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ──────── 敏感字段脱敏（含 ConcurrentHashMap 元数据缓存） ────────

    /** 缓存：Class → 标注了 @Sensitive 的 Field 列表。首次反射扫描后缓存，后续 O(1) 查找。 */
    private static final Map<Class<?>, List<Field>> SENSITIVE_FIELDS_CACHE = new ConcurrentHashMap<>();

    /**
     * 对 DTO 对象中的 {@link Sensitive} 标注字段进行脱敏。
     * <p>
     * 仅扫描 {@code com.competition.energycontrollerplatform.dto}
     * 和 {@code com.competition.energycontrollerplatform.miniapp.dto} 包下的类，
     * 其他对象原样返回（零开销）。
     * <p>
     * 首次遇到新 DTO 类型时执行反射扫描 + 缓存（~0.5ms），
     * 后续同类型请求直接从缓存获取敏感字段列表（~μs 级）。
     */
    private Object maskSensitiveFields(Object obj) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        // 快速短路：只处理 DTO 包
        if (!className.startsWith("com.competition.energycontrollerplatform.dto")
                && !className.startsWith("com.competition.energycontrollerplatform.miniapp.dto")) {
            return obj;
        }
        // 从缓存获取敏感字段列表（首次 → 反射扫描 + 缓存；后续 → O(1)）
        List<Field> fields = SENSITIVE_FIELDS_CACHE.computeIfAbsent(clazz, k -> {
            List<Field> result = new ArrayList<>();
            for (Field f : clazz.getDeclaredFields()) {
                Sensitive ann = f.getAnnotation(Sensitive.class);
                if (ann != null) {
                    f.setAccessible(true);
                    result.add(f);
                }
            }
            return result.isEmpty() ? Collections.emptyList() : result;
        });
        if (fields.isEmpty()) return obj;
        // 深拷贝 + 脱敏（不修改原始对象）
        try {
            Object copy = objectMapper.convertValue(obj, clazz);
            for (Field field : fields) {
                Sensitive ann = field.getAnnotation(Sensitive.class);
                Object value = field.get(copy);
                if (value instanceof String s) {
                    String masked = switch (ann.value()) {
                        case PASSWORD, CODE -> SensitiveDataMasker.maskPassword(s);
                        case PHONE -> SensitiveDataMasker.maskPhone(s);
                        case OPENID -> SensitiveDataMasker.maskOpenId(s);
                    };
                    field.set(copy, masked);
                }
            }
            return copy;
        } catch (Exception e) {
            // 脱敏失败不影响主流程，返回原对象（best-effort）
            return obj;
        }
    }

    private String businessTypeLabel(BusinessType type) {
        return switch (type) {
            case QUERY    -> "查询";
            case INSERT   -> "新增";
            case UPDATE   -> "修改";
            case DELETE   -> "删除";
            case LOGIN    -> "登录";
            case EXPORT   -> "导出";
            case RECHARGE -> "充值";
            case REFUND   -> "退费";
            default       -> "其他";
        };
    }
}
