package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentPageRequest;
import com.competition.energycontrollerplatform.dto.BalanceAdjustmentVO;
import com.competition.energycontrollerplatform.service.BalanceAdjustmentService;
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
 * 余额调整 Controller。
 * <p>
 * 提供余额调整（调账）记录的分页列表查询、详情查询、统计聚合、Excel 导出功能。
 * 权限隔离：ADMIN 可查看全局，非 ADMIN 仅可查看自身关联商户的记录。
 */
@Tag(name = "余额退补 - 余额退补", description = "余额调整记录查询、统计、导出接口")
@RestController
@RequestMapping("/api/v1/balance-adjustment")
@RequiredArgsConstructor
@Validated
public class BalanceAdjustmentController {

    private final BalanceAdjustmentService balanceAdjustmentService;

    @Operation(summary = "余额调整记录分页查询", description = "支持按时段、按商户、按调整类型（INCREASE/DECREASE）、关键字过滤。"
            + "非 ADMIN 角色仅可查看自身关联商户的记录。")
    @GetMapping("/list")
    public Result<IPage<BalanceAdjustmentVO>> list(@Valid BalanceAdjustmentPageRequest request) {
        IPage<BalanceAdjustmentVO> page = balanceAdjustmentService.listAdjustments(request);
        return Result.success(page);
    }

    @Operation(summary = "余额调整详情", description = "根据ID查询单条余额调整记录的详细信息。"
            + "非 ADMIN 角色仅可查看自身关联商户的记录。")
    @GetMapping("/{id}")
    public Result<BalanceAdjustmentVO> detail(
            @Parameter(description = "调整记录ID", required = true, example = "1")
            @PathVariable Long id) {
        BalanceAdjustmentVO vo = balanceAdjustmentService.getAdjustmentById(id);
        if (vo == null) {
            return Result.error(404, "余额调整记录不存在或无权查看");
        }
        return Result.success(vo);
    }

    @Operation(summary = "余额调整统计", description = "按日聚合余额调整总金额、笔数、涉及商户数。"
            + "聚合在 MySQL 层通过 DATE_FORMAT + SUM + GROUP BY 完成。")
    @GetMapping("/summary")
    public Result<List<com.competition.energycontrollerplatform.dto.BalanceAdjustmentSummaryVO>> summary(
            @Valid BalanceAdjustmentPageRequest request) {
        List<com.competition.energycontrollerplatform.dto.BalanceAdjustmentSummaryVO> summary =
                balanceAdjustmentService.getAdjustmentSummary(request);
        return Result.success(summary);
    }

    @Operation(summary = "余额调整记录导出", description = "按筛选条件导出余额调整记录为 Excel 文件。"
            + "使用 EasyExcel 流式写入，支持大数据量导出不 OOM。"
            + "导出字段：调整编号、商户名称、表号、调整金额、调整类型、调账原因、操作员、调整时间、备注、创建时间。")
    @GetMapping("/export")
    public void export(@Valid BalanceAdjustmentPageRequest request, HttpServletResponse response) {
        balanceAdjustmentService.exportAdjustments(response, request);
    }

    // ──────────────────────── 余额退补统一执行 ────────────────────────

    @Operation(summary = "余额退补统一执行", description = "串联[调整电价-选择表计-预览-执行]全流程。"
            + "<p><b>支持两种执行类型：</b>"
            + "<ul>"
            + "<li>PRICE_ADJUST — 调整电价并执行余额退补：先修改表计电价，再根据金额变动调整余额</li>"
            + "<li>BALANCE_ADJUST — 直接余额调整：对商户账户进行增减操作，金额正=退费/增加，负=补收/扣减</li>"
            + "</ul>"
            + "<p><b>预览模式：</b>设置 previewOnly=true 仅计算预览结果不执行，用于确认界面展示。"
            + "<p><b>工程保障：</b>"
            + "<ul>"
            + "<li>@Transactional 事务确保电价变更 + 余额变动 + 流水写入原子一致</li>"
            + "<li>BigDecimal 精确计算</li>"
            + "<li>SQL 原子更新 SET balance = balance + ? AND balance >= ?（扣减时）</li>"
            + "<li>写入 t_balance_adjustment 审计日志 + t_account_flow 资金流水</li>"
            + "</ul>")
    @PostMapping("/execute")
    public Result<BalanceAdjustmentVO> execute(
            @Valid @RequestBody com.competition.energycontrollerplatform.dto.BalanceAdjustmentExecuteRequest request) {
        BalanceAdjustmentVO result = balanceAdjustmentService.executeAdjustment(request);
        if (Boolean.TRUE.equals(request.getPreviewOnly())) {
            return Result.success("预览结果（未实际执行）", result);
        }
        return Result.success("余额退补执行成功，调整编号：" + result.getAdjustmentNo(), result);
    }
}
