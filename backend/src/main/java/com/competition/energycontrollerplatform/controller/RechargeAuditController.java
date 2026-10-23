package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.RechargeAuditPageRequest;
import com.competition.energycontrollerplatform.dto.RechargeAuditRequest;
import com.competition.energycontrollerplatform.miniapp.dto.RechargeHistoryVO;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.RechargeAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端 — 充值审核控制器。
 * <p>
 * 小程序用户提交充值申请后，管理员在此审核通过或拒绝。
 * 审核通过后自动执行充值入账：创建充值订单、增加商户余额。
 */
@Tag(name = "管理端-充值审核", description = "充值申请分页查询、审核通过/拒绝")
@RestController
@RequestMapping("/api/v1/recharge-audit")
@RequiredArgsConstructor
@Validated
public class RechargeAuditController {

    private final RechargeAuditService rechargeAuditService;

    @GetMapping("/page")
    @Operation(summary = "充值审核列表", description = "分页查询所有充值申请，可按审核状态筛选。PENDING 优先排列。")
    public Result<IPage<RechargeHistoryVO>> page(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "审核状态: PENDING/APPROVED/REJECTED，不传=全部") @RequestParam(required = false) String auditStatus,
            @Parameter(description = "搜索关键词（商户名/表号/申请编号）") @RequestParam(required = false) String keyword) {

        RechargeAuditPageRequest request = new RechargeAuditPageRequest();
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        request.setAuditStatus(auditStatus);
        request.setKeyword(keyword);

        IPage<RechargeHistoryVO> page = rechargeAuditService.getAuditPage(request);
        return Result.success(page);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过", description = "审核通过充值申请。系统自动创建充值订单并增加商户余额。")
    @Log(title = "充值审核", businessType = BusinessType.RECHARGE)
    public Result<Map<String, Object>> approve(
            @Parameter(description = "充值申请ID") @PathVariable Long id,
            @Valid @RequestBody RechargeAuditRequest auditRequest) {

        Long auditUserId = SecurityUserContext.getCurrentUserId();
        String orderNo = rechargeAuditService.approve(id, auditUserId, auditRequest.getAuditRemark());

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", id);
        data.put("auditStatus", "APPROVED");
        data.put("orderNo", orderNo);
        return Result.success("审核通过，充值已到账", data);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审核拒绝", description = "拒绝充值申请，需填写拒绝原因。")
    public Result<Map<String, Object>> reject(
            @Parameter(description = "充值申请ID") @PathVariable Long id,
            @Valid @RequestBody RechargeAuditRequest auditRequest) {

        Long auditUserId = SecurityUserContext.getCurrentUserId();
        rechargeAuditService.reject(id, auditUserId, auditRequest.getAuditRemark());

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", id);
        data.put("auditStatus", "REJECTED");
        return Result.success("已拒绝该充值申请", data);
    }
}
