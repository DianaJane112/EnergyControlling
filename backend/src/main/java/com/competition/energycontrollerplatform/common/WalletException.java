package com.competition.energycontrollerplatform.common;

import lombok.Getter;

/**
 * 资金操作异常。
 * 余额不足、并发竞争导致更新失败等资金相关错误时抛出，
 * 由 {@link GlobalExceptionHandler} 统一捕获并触发事务回滚。
 */
@Getter
public class WalletException extends RuntimeException {

    /** 余额不足 */
    public static final int CODE_INSUFFICIENT_BALANCE = 4001;
    /** 并发竞争/更新冲突 */
    public static final int CODE_CONCURRENCY_CONFLICT = 4002;
    /** 金额参数非法 */
    public static final int CODE_INVALID_AMOUNT = 4003;
    /** 商户不存在 */
    public static final int CODE_CUSTOMER_NOT_FOUND = 4004;

    private final Integer code;

    public WalletException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /** 余额不足 */
    public static WalletException insufficientBalance(Long customerId, java.math.BigDecimal balance, java.math.BigDecimal required) {
        return new WalletException(CODE_INSUFFICIENT_BALANCE,
                String.format("商户[%d]余额不足：当前余额 %s，需要 %s", customerId, balance.toPlainString(), required.toPlainString()));
    }

    /** 并发冲突 */
    public static WalletException concurrencyConflict(Long customerId, String operation) {
        return new WalletException(CODE_CONCURRENCY_CONFLICT,
                String.format("商户[%d] %s 发生并发竞争，请重试", customerId, operation));
    }

    /** 金额非法 */
    public static WalletException invalidAmount(String message) {
        return new WalletException(CODE_INVALID_AMOUNT, message);
    }

    /** 商户不存在 */
    public static WalletException customerNotFound(Long customerId) {
        return new WalletException(CODE_CUSTOMER_NOT_FOUND,
                String.format("商户[%d]不存在或已注销", customerId));
    }
}
