package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.AdjustBalanceRequest;
import com.competition.energycontrollerplatform.dto.RechargeRequest;
import com.competition.energycontrollerplatform.dto.RefundRequest;
import com.competition.energycontrollerplatform.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 资金计费 Controller
 * 充值 / 退费 / 调账
 */
@Tag(name = "客服收费 - 人工缴费", description = "商户级充值、退费、余额调整（调账）操作接口")
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @Operation(summary = "商户充值", description = "为商户账户充值，增加余额，记录充值订单和资金流水")
    @Log(title = "资金计费", businessType = BusinessType.RECHARGE)
    @PostMapping("/recharge")
    public Result<String> recharge(@Valid @RequestBody RechargeRequest request) {
        String orderNo = billingService.recharge(request);
        return Result.success("充值成功，订单号：" + orderNo, orderNo);
    }

    @Operation(summary = "商户退费", description = "扣减商户账户余额，需校验余额充足，记录退费单和资金流水")
    @Log(title = "资金计费", businessType = BusinessType.REFUND)
    @PostMapping("/refund")
    public Result<String> refund(@Valid @RequestBody RefundRequest request) {
        String refundNo = billingService.refund(request);
        return Result.success("退费成功，退费编号：" + refundNo, refundNo);
    }

    @Operation(summary = "余额调整（调账）", description = "运维手动修正商户余额，支持正负金额，必须填写调账原因")
    @PostMapping("/adjust-balance")
    public Result<String> adjustBalance(@Valid @RequestBody AdjustBalanceRequest request) {
        String adjustmentNo = billingService.adjustBalance(request);
        return Result.success("调账成功，调整编号：" + adjustmentNo, adjustmentNo);
    }
}
