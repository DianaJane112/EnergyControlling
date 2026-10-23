package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 退费管理 Controller。
 * <p>
 * 提供退费明细的分页列表查询、详情查询、退费预览、Excel 导出功能。
 * 权限隔离：ADMIN 可查看全局，非 ADMIN 仅可查看自身关联商户的记录。
 */
@Tag(name = "财务管理 - 退费明细", description = "退费明细查询、退费预览、退费明细导出接口")
@RestController
@RequestMapping("/api/v1/refund")
@RequiredArgsConstructor
@Validated
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "退费明细分页查询", description = "支持按时段、按商户、关键字过滤退费明细。"
            + "数据来源：t_refund_detail 联查 t_customer、t_meter。"
            + "非 ADMIN 角色强制隔离。")
    @GetMapping("/detail-list")
    public Result<IPage<RefundDetailVO>> detailList(@Valid FinancialPageRequest request) {
        IPage<RefundDetailVO> page = refundService.listRefundDetails(request);
        return Result.success(page);
    }

    @Operation(summary = "退费明细详情", description = "根据ID查询单条退费明细的详细信息。"
            + "包含商户名称、表号等关联信息。非 ADMIN 角色仅可查看自身关联商户的记录。")
    @GetMapping("/detail/{id}")
    public Result<RefundDetailVO> detail(
            @Parameter(description = "退费明细ID", required = true, example = "1")
            @PathVariable Long id) {
        RefundDetailVO vo = refundService.getRefundDetailById(id);
        if (vo == null) {
            return Result.error(404, "退费记录不存在或无权查看");
        }
        return Result.success(vo);
    }

    @Operation(summary = "退费预览", description = "在正式退费前，根据表计ID和拟退费金额预览退费结果。"
            + "展示表计信息、商户信息、当前余额/电价、退费后余额、是否触发欠费停机等。"
            + "支持传入 adjustedPrice 参数预览不同电价下的余额变化。")
    @PostMapping("/preview")
    public Result<RefundPreviewVO> preview(@Valid @RequestBody RefundPreviewRequest request) {
        RefundPreviewVO vo = refundService.previewRefund(request);
        return Result.success(vo);
    }

    @Operation(summary = "退费明细导出", description = "按筛选条件导出退费明细为 Excel 文件。"
            + "使用 EasyExcel 流式写入，支持大数据量导出不 OOM。"
            + "导出字段：退费编号、商户名称、表号、退费金额、退费类型、退费原因、操作员、退费时间、备注、创建时间。")
    @GetMapping("/export-detail")
    public void exportDetail(@Valid FinancialPageRequest request, HttpServletResponse response) {
        refundService.exportRefundDetails(response, request);
    }
}
