package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.service.FinancialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 财务管理 Controller。
 * <p>
 * 提供充值明细分页、充值统计聚合、退费明细分页三大核心接口。
 * 所有金额字段使用 {@link java.math.BigDecimal}，统计聚合在 MySQL 层完成。
 */
@Tag(name = "财务管理 - 充值明细", description = "充值明细分页查询、充值统计聚合、缴费凭证打印、Excel导出接口")
@RestController
@RequestMapping("/api/v1/financial")
@RequiredArgsConstructor
@Validated
public class FinancialController {

    private final FinancialService financialService;

    @Operation(summary = "充值明细分页查询", description = "支持按时段、按商户、关键字（订单号/商户名）过滤。"
            + "非 ADMIN 角色仅可查看自身关联商户的充值记录。")
    @GetMapping("/deposit-log")
    public Result<IPage<DepositLogVO>> depositLog(@Valid FinancialPageRequest request) {
        IPage<DepositLogVO> page = financialService.listDepositLog(request);
        return Result.success(page);
    }

    @Operation(summary = "充值统计聚合", description = "按日/月/年维度聚合充值总额、笔数、商户数。"
            + "聚合在 MySQL 层通过 DATE_FORMAT + SUM + GROUP BY 完成，不捞回 JVM。")
    @GetMapping("/deposit-summary")
    public Result<List<DepositSummaryVO>> depositSummary(@Valid DepositSummaryRequest request) {
        List<DepositSummaryVO> summary = financialService.getDepositSummary(request);
        return Result.success(summary);
    }

    @Tag(name = "财务管理 - 退费明细")
    @Operation(summary = "退费明细分页查询", description = "支持按时段、按商户过滤退费日志。"
            + "数据来源：t_refund_log 联查 t_customer。非 ADMIN 角色强制隔离。")
    @GetMapping("/refund-log")
    public Result<IPage<RefundLogVO>> refundLog(@Valid FinancialPageRequest request) {
        IPage<RefundLogVO> page = financialService.listRefundLog(request);
        return Result.success(page);
    }

    // ──────────────────────── 打印缴费记录 ────────────────────────

    @Operation(summary = "打印缴费记录", description = "根据充值订单 ID 查询单条充值明细，用于打印缴费凭证。"
            + "非 ADMIN 角色仅可打印自身关联商户的充值记录。")
    @GetMapping("/deposit-log/{id}/print")
    public Result<DepositLogVO> printDepositLog(
            @Parameter(description = "充值订单ID", example = "1")
            @PathVariable Long id) {
        DepositLogVO vo = financialService.getDepositLogById(id);
        if (vo == null) {
            throw BusinessException.of(404, "充值记录不存在或无权访问");
        }
        return Result.success("查询成功", vo);
    }

    // ──────────────────────── Excel 导出 ────────────────────────

    @Operation(summary = "充值明细分页导出", description = "按筛选条件导出充值明细为 Excel 文件。"
            + "使用 EasyExcel 流式写入，支持大数据量导出不 OOM。"
            + "导出字段：订单号、商户名称、表号、充值金额、支付金额、到账金额、余额、操作员、项目名称、备注、充值时间。")
    @GetMapping("/export/deposit-log")
    public void exportDepositLog(@Valid FinancialPageRequest request, HttpServletResponse response) {
        financialService.exportDepositLog(response, request);
    }

    @Operation(summary = "充值统计导出", description = "按日/月/年粒度导出充值统计数据为 Excel 文件。"
            + "导出字段：统计时段、充值总金额、充值笔数、涉及商户数。")
    @GetMapping("/export/deposit-summary")
    public void exportDepositSummary(@Valid DepositSummaryRequest request, HttpServletResponse response) {
        financialService.exportDepositSummary(response, request);
    }

    @Tag(name = "财务管理 - 退费明细")
    @Operation(summary = "退费明细导出", description = "按筛选条件导出退费明细为 Excel 文件。"
            + "使用 EasyExcel 流式写入，支持大数据量导出不 OOM。"
            + "导出字段：流水号、商户名称、退费前余额、退费金额、退费后余额、退费原因、操作员、退费时间、备注。")
    @GetMapping("/export/refund-log")
    public void exportRefundLog(@Valid FinancialPageRequest request, HttpServletResponse response) {
        financialService.exportRefundLog(response, request);
    }
}
