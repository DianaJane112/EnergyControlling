package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.dto.AdjustBalanceRequest;
import com.competition.energycontrollerplatform.dto.RechargeRequest;
import com.competition.energycontrollerplatform.dto.RefundRequest;

/**
 * 资金计费服务接口
 * 充值、退费、调账三大核心资金操作
 */
public interface BillingService {

    /**
     * 充值：增加商户余额，记录充值订单和资金流水
     *
     * @param request 充值请求（商户ID、金额、操作人）
     * @return 充值订单号
     */
    String recharge(RechargeRequest request);

    /**
     * 退费：扣减商户余额，需校验余额充足，记录退费单和资金流水
     *
     * @param request 退费请求（商户ID、金额、操作人、原因）
     * @return 退费编号
     */
    String refund(RefundRequest request);

    /**
     * 调账：运维手动修正余额，支持正负金额，必须记录原因
     *
     * @param request 调账请求（商户ID、金额、操作人、原因）
     * @return 调整编号
     */
    String adjustBalance(AdjustBalanceRequest request);
}
