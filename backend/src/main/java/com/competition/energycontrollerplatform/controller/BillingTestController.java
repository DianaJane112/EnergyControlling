package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.integration.scheduler.AutomatedBillingScheduler;
import com.competition.energycontrollerplatform.service.EnergyConsumptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 计费测试接口（仅开发环境可用，生产环境不暴露）。
 */
@Profile("dev")
@Tag(name = "计费测试", description = "手动触发自动扣费，仅用于测试验证")
@RestController
@RequestMapping("/api/v1/billing/test")
@RequiredArgsConstructor
public class BillingTestController {

    private final EnergyConsumptionService energyConsumptionService;
    private final AutomatedBillingScheduler automatedBillingScheduler;

    @Operation(summary = "触发全局自动扣费调度", description = "立即执行一次完整的分页逐台扣费（等同于定时任务）")
    @PostMapping("/run-all")
    public Result<String> runAll() {
        automatedBillingScheduler.executeAutomatedBilling();
        return Result.success("全局扣费调度已执行完毕，请查看控制台日志", "done");
    }

    @Operation(summary = "单表扣费", description = "对指定表计ID执行一次读数差→计费→扣款闭环")
    @PostMapping("/single-meter/{meterId}")
    public Result<String> singleMeter(@PathVariable Long meterId,
                                      @RequestParam(defaultValue = "0.85") BigDecimal price) {
        BigDecimal cost = energyConsumptionService.calculateAndDebit(meterId, price);
        if (cost.compareTo(BigDecimal.ZERO) > 0) {
            return Result.success("扣费成功，金额：" + cost.toPlainString() + " 元", cost.toPlainString());
        }
        return Result.success("无需扣费（读数无增长或无绑定商户）", "0");
    }
}
