package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.dto.DepositRequest;
import com.competition.energycontrollerplatform.dto.ManualRefundRequest;

/**
 * 人工收费服务接口。
 * <p>
 * 提供人工缴费（充值）与人工退费两大核心资金操作。
 * 所有操作均在同一数据库事务内完成"余额变更 + 流水写入"，
 * 使用原生 SQL 原子更新防止高并发下的数据覆盖。
 */
public interface ChargeService {

    /**
     * 人工缴费（充值）。
     * <p>
     * 流程：
     * <ol>
     *   <li>权限校验：非 ADMIN 强制使用当前登录用户的关联商户</li>
     *   <li>原子增加商户余额：UPDATE t_customer SET balance = balance + #{amount}</li>
     *   <li>写入资金流水（trade_type=5 人工缴费）</li>
     *   <li>检查余额是否触发警告额度边界，若触发则更新表计状态</li>
     * </ol>
     *
     * @param request 缴费请求（商户ID、金额、表计ID可空）
     * @return 流水号
     */
    String deposit(DepositRequest request);

    /**
     * 人工退费。
     * <p>
     * 流程：
     * <ol>
     *   <li>权限校验：非 ADMIN 仅可操作自身商户</li>
     *   <li>校验表计存在且已绑定商户</li>
     *   <li>原子扣减余额（SQL 含 AND balance >= amount 防负数）</li>
     *   <li>写入 t_refund_log 退费日志</li>
     *   <li>写入 t_account_flow 资金流水（trade_type=3 退费）</li>
     *   <li>写入 t_refund_detail 退费明细</li>
     * </ol>
     *
     * @param request 退费请求（表计ID、金额、原因必填）
     * @return 退费流水号
     */
    String manualRefund(ManualRefundRequest request);
}
