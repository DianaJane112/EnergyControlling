package com.competition.energycontrollerplatform.miniapp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.Idempotent;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeHistoryVO;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeSubmitRequest;
import com.competition.energycontrollerplatform.miniapp.security.MiniappSecurityContext;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppRechargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 小程序 - 充值控制器
 */
@RestController
@RequestMapping("/api/v1/miniapp/recharge")
@RequiredArgsConstructor
@Tag(name = "小程序-充值", description = "充值申请提交、充值历史查询")
public class MiniAppRechargeController {

    private final MiniAppRechargeService miniAppRechargeService;

    @Idempotent(ttlSeconds = 120)
    @PostMapping
    @Operation(summary = "提交充值申请", description = "用户发起虚拟充值申请，等待管理员审核。需携带 X-Idempotency-Key 请求头防止重复提交。")
    public Result<Map<String, Object>> submitRecharge(@Valid @RequestBody RechargeSubmitRequest request) {
        Long userId = MiniappSecurityContext.getCurrentUserId();
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();

        String requestNo = miniAppRechargeService.submitRecharge(userId, customerId, request);

        Map<String, Object> data = new HashMap<>();
        data.put("requestNo", requestNo);
        data.put("auditStatus", "PENDING");
        data.put("amount", request.getAmount());
        return Result.success("充值申请已提交，等待管理员审核", data);
    }

    @GetMapping("/history")
    @Operation(summary = "充值历史", description = "分页查询充值申请历史记录，支持按审核状态筛选")
    public Result<IPage<RechargeHistoryVO>> rechargeHistory(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "审核状态: PENDING/APPROVED/REJECTED，不传=全部") @RequestParam(required = false) String auditStatus) {

        Long userId = MiniappSecurityContext.getCurrentUserId();
        IPage<RechargeHistoryVO> page = miniAppRechargeService.getRechargeHistory(userId, pageNum, pageSize, auditStatus);
        return Result.success(page);
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "充值申请详情", description = "查看单条充值申请的详细信息")
    public Result<RechargeHistoryVO> rechargeDetail(@PathVariable Long requestId) {
        Long userId = MiniappSecurityContext.getCurrentUserId();
        RechargeHistoryVO vo = miniAppRechargeService.getRechargeDetail(requestId, userId);
        return Result.success(vo);
    }
}
