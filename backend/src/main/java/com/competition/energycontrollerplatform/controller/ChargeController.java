package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.service.ChargeService;
import com.competition.energycontrollerplatform.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 人工收费 Controller（缴费 / 退费）。
 * <p>
 * 与 {@code BillingController} 的区别：
 * <ul>
 *   <li>{@code BillingController} — 面向商户级别的充值/退费/调账（customer 维度）</li>
 *   <li>{@code ChargeController} — 面向表计级别的人工缴费/退费（meter 维度），
 *       联动更新表计状态位与硬件阀控 Hook</li>
 * </ul>
 */
@Tag(name = "客服收费 - 人工缴费", description = "表计级人工缴费（充值）、人工退费、退费预览接口")
@RestController
@RequestMapping("/api/v1/charge")
@RequiredArgsConstructor
@Validated
public class ChargeController {

    private final ChargeService chargeService;
    private final RefundService refundService;

    @Operation(summary = "人工缴费", description = "为商户账户人工充值，原子增加余额并记录资金流水。"
            + "非 ADMIN 角色强制使用当前登录商户，无视前端传入的 customerId。")
    @Log(title = "人工收费", businessType = BusinessType.RECHARGE)
    @PostMapping("/deposit")
    public Result<String> deposit(@Valid @RequestBody DepositRequest request) {
        String flowNo = chargeService.deposit(request);
        return Result.success("缴费成功，流水号：" + flowNo, flowNo);
    }

    @Operation(summary = "人工退费", description = "从商户账户扣减退费金额。"
            + "原子 SQL 校验余额充足，退费成功后写入 t_refund_log 审计日志。"
            + "退费原因必填，用于审计追溯。")
    @Log(title = "人工收费", businessType = BusinessType.REFUND)
    @PostMapping("/refund")
    public Result<String> refund(@Valid @RequestBody ManualRefundRequest request) {
        String flowNo = chargeService.manualRefund(request);
        return Result.success("退费成功，流水号：" + flowNo, flowNo);
    }

    @Operation(summary = "退费预览（表计级）", description = "在正式退费前预览退费结果。"
            + "根据表计ID和拟退费金额，展示：表计信息、商户信息、当前余额/电价、"
            + "退费后余额、是否触发欠费停机等。"
            + "支持传入 adjustedPrice 参数预览不同电价下的余额变化（调整电价场景）。")
    @PostMapping("/refund/preview")
    public Result<RefundPreviewVO> refundPreview(@Valid @RequestBody RefundPreviewRequest request) {
        RefundPreviewVO vo = refundService.previewRefund(request);
        return Result.success(vo);
    }
}
