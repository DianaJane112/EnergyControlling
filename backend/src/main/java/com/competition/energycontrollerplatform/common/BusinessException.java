package com.competition.energycontrollerplatform.common;

import lombok.Getter;

/**
 * 自定义业务异常。
 * 在业务逻辑中遇到可预期的错误（如数据不存在、余额不足等）时主动抛出，
 * 由 {@link GlobalExceptionHandler} 统一捕获并封装为 {@link Result} 返回。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        // 业务异常默认为 400（客户端错误），而非 500（服务器错误）
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 快捷构造：业务异常（500）
     */
    public static BusinessException of(String message) {
        return new BusinessException(Result.CODE_ERROR, message);
    }

    /**
     * 快捷构造：自定义状态码
     */
    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }
}
