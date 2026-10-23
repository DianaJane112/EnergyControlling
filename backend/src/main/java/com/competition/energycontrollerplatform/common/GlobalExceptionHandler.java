package com.competition.energycontrollerplatform.common;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.util.stream.Collectors;

/**
 * 全局异常拦截器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ────────────── 业务异常 ──────────────

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        if (e.getCode() != null) {
            response.setStatus(e.getCode());
        }
        return Result.error(e.getCode(), e.getMessage());
    }

    // ────────────── 资金异常 ──────────────

    @ExceptionHandler(WalletException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleWalletException(WalletException e) {
        log.warn("资金异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // ────────────── 参数校验 ──────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = String.format("参数 [%s] 类型错误，期望 %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        log.warn("参数类型转换失败: {}", msg);
        return Result.error(400, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error(400, "请求体格式错误，请检查 JSON 结构");
    }

    // ────────────── Spring Security 认证异常 ──────────────

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleBadCredentials(org.springframework.security.authentication.BadCredentialsException e) {
        log.warn("认证失败: {}", e.getMessage());
        return Result.error(Result.CODE_UNAUTHORIZED, "用户名或密码错误");
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleDisabled(org.springframework.security.authentication.DisabledException e) {
        log.warn("账号已禁用: {}", e.getMessage());
        return Result.error(Result.CODE_FORBIDDEN, "账号已被禁用，请联系管理员");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(org.springframework.security.access.AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(Result.CODE_FORBIDDEN, "权限不足，无法访问该资源");
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthentication(org.springframework.security.core.AuthenticationException e) {
        log.warn("认证异常: {}", e.getMessage());
        return Result.error(Result.CODE_UNAUTHORIZED, "认证失败，请重新登录");
    }

    // ────────────── HTTP 方法不支持 ──────────────

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {} {}", e.getMethod(), e.getMessage());
        return Result.error(405, "请求方式不正确，请检查接口调用方式");
    }

    // ────────────── 文件上传过大 ──────────────

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMaxUploadSizeExceeded(
            org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        long maxSizeMb = e.getMaxUploadSize() > 0 ? e.getMaxUploadSize() / 1024 / 1024 : -1;
        log.warn("文件上传过大: {} (max: {}MB)", e.getMessage(), maxSizeMb);
        return Result.error(400, "上传文件过大，单次上传不能超过 50MB，请分批导入或压缩文件");
    }

    // ────────────── 静态资源缺失（favicon 等）降级为 204 ──────────────

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Result<Void> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        // 不打印日志，避免噪音
        return Result.error(204, null);
    }

    // ────────────── 数据库约束异常 ──────────────

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("数据重复: {}", e.getMessage());
        String msg = extractDuplicateKeyMessage(e);
        return Result.error(400, msg);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性约束违反: {}", e.getMessage());
        String msg = extractDataIntegrityMessage(e);
        return Result.error(400, msg);
    }

    // ────────────── 兜底 ──────────────

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        // 生产环境不暴露内部异常详情，仅返回通用提示
        return Result.error(Result.CODE_ERROR, "系统繁忙，请稍后重试");
    }

    // ────────────── 私有辅助方法 ──────────────

    /**
     * 从 {@link DuplicateKeyException} 中提取字段名和值，返回友好提示。
     */
    private String extractDuplicateKeyMessage(DuplicateKeyException e) {
        String msg = e.getMessage();
        if (msg == null) return "数据重复，请检查唯一字段";

        // 尝试提取 MySQL 错误 "Duplicate entry 'VALUE' for key 'TABLE.KEY'"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Duplicate entry '([^']*)' for key '([^']+)'")
                .matcher(msg);
        if (m.find()) {
            String value = m.group(1);
            String key = m.group(2);
            // 去掉表名前缀，只保留字段名
            if (key.contains(".")) {
                key = key.substring(key.lastIndexOf('.') + 1);
            }
            return String.format("数据重复：字段 [%s] 的值 [%s] 已存在，请使用其他值", key, value);
        }
        return "数据重复，请检查唯一字段（如编号、名称等）";
    }

    /**
     * 从 {@link DataIntegrityViolationException} 中提取友好提示。
     */
    private String extractDataIntegrityMessage(DataIntegrityViolationException e) {
        String msg = e.getMessage();
        if (msg == null) return "数据不合法，请检查输入";

        if (msg.contains("Data too long")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("Data too long for column '(\\w+)'")
                    .matcher(msg);
            if (m.find()) {
                return String.format("字段 [%s] 的值过长，请缩短后重试", m.group(1));
            }
            return "输入值过长，请检查字段长度";
        }
        if (msg.contains("cannot be null")) {
            // 从 MySQL 错误中提取列名，如 "Column 'received_amount' cannot be null"
            java.util.regex.Matcher colMatcher = java.util.regex.Pattern
                    .compile("Column '(\\w+)' cannot be null")
                    .matcher(msg);
            if (colMatcher.find()) {
                return String.format("必填字段 [%s] 未填写，请检查", colMatcher.group(1));
            }
            return "存在必填字段未填写，请检查";
        }
        if (msg.contains("doesn't have a default value")) {
            // 从 MySQL 错误中提取列名，如 "Field 'parent_meter_id' doesn't have a default value"
            java.util.regex.Matcher colMatcher = java.util.regex.Pattern
                    .compile("Field '(\\w+)' doesn't have a default value")
                    .matcher(msg);
            if (colMatcher.find()) {
                return String.format("数据库字段 [%s] 缺少默认值，请执行对应的数据库迁移脚本", colMatcher.group(1));
            }
            return "数据库缺少字段默认值，请执行对应的数据库迁移脚本";
        }
        return "数据不合法，请检查输入";
    }
}
