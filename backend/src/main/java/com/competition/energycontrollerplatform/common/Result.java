package com.competition.energycontrollerplatform.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 统一后端响应对象
 *
 * @param <T> 业务数据体类型
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一响应结果")
public class Result<T> {

    @Schema(description = "业务状态码", example = "200")
    public static final int CODE_SUCCESS = 200;
    @Schema(description = "未认证")
    public static final int CODE_UNAUTHORIZED = 401;
    @Schema(description = "无权限")
    public static final int CODE_FORBIDDEN = 403;
    @Schema(description = "系统异常")
    public static final int CODE_ERROR = 500;

    @Schema(description = "状态码")
    private final Integer code;

    @Schema(description = "提示信息")
    private final String msg;

    @Schema(description = "业务数据")
    private final T data;

    @Schema(description = "时间戳")
    private final Long timestamp;

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ────────────── 成功 ──────────────

    public static <T> Result<T> success() {
        return new Result<>(CODE_SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CODE_SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(CODE_SUCCESS, msg, data);
    }

    public static Result<Void> success(String msg) {
        return new Result<>(CODE_SUCCESS, msg, null);
    }

    // ────────────── 失败 ──────────────

    public static <T> Result<T> error(String msg) {
        return new Result<>(CODE_ERROR, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    // ────────────── 状态判断 ──────────────

    public boolean isSuccess() {
        return this.code != null && this.code == CODE_SUCCESS;
    }
}
