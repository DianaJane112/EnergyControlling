package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 物业管理 / 即时账单 Controller。
 * <p>提供即时账单生成、分页列表、导出、批量操作、缴费轨迹等物业管理功能。</p>
 */
@Tag(name = "物业服务 - 即时账单", description = "即时账单生成、列表查询、导出、批量操作、缴费轨迹接口")
@RestController
@RequestMapping("/api/v1/property")
@RequiredArgsConstructor
@Validated
public class PropertyController {

    private final PropertyService propertyService;

    // ──────────────────────── 即时账单详情 ────────────────────────

    @Operation(summary = "生成即时账单（单表计）", description = "实时获取指定表计的最新读数与上次结算读数，"
            + "在内存中用 BigDecimal 计算差值 × 倍率 × 单价 = 账单金额。"
            + "返回：当前读数、上次读数、消耗用量、账单金额、计算时间戳。")
    @GetMapping("/instant-bill/{meterId}")
    public Result<InstantBillVO> instantBill(
            @Parameter(description = "表计ID", required = true, example = "1")
            @PathVariable Long meterId) {
        InstantBillVO bill = propertyService.generateInstantBill(meterId);
        return Result.success(bill);
    }

    // ──────────────────────── 即时账单数据列表 ────────────────────────

    @Operation(summary = "即时账单分页列表", description = "支持按商户、表计号、账单分类、时间范围、金额范围等条件筛选。"
            + "非 ADMIN 角色强制隔离，仅可查看自身关联商户数据。")
    @GetMapping("/instant-bill/list")
    public Result<IPage<InstantBillListVO>> list(@Valid InstantBillPageRequest request) {
        IPage<InstantBillListVO> page = propertyService.listInstantBills(request);
        return Result.success(page);
    }

    @Operation(summary = "即时账单统计汇总", description = "返回总账单数、正常数、欠费数、总余额、今日消费等汇总统计。")
    @GetMapping("/instant-bill/stats")
    public Result<Map<String, Object>> stats(@Valid InstantBillPageRequest request) {
        Map<String, Object> stats = propertyService.getInstantBillStats(request);
        return Result.success(stats);
    }

    @Operation(summary = "导出即时账单列表（Excel）", description = "按筛选条件导出即时账单为 Excel 文件。"
            + "使用 EasyExcel 流式写入，支持大数据量导出。")
    @Log(title = "即时账单", businessType = BusinessType.EXPORT)
    @GetMapping("/instant-bill/export")
    public void exportList(@Valid InstantBillPageRequest request, HttpServletResponse response) {
        propertyService.exportInstantBills(response, request);
    }

    // ──────────────────────── 批量操作 ────────────────────────

    @Operation(summary = "批量创建账单", description = "为指定表计列表批量生成即时账单。仅 ADMIN 可执行。")
    @Log(title = "即时账单", businessType = BusinessType.INSERT)
    @PostMapping("/instant-bill/batch-create")
    public Result<BatchOperationResponse> batchCreate(@Valid @RequestBody BatchBillCreateRequest request) {
        BatchOperationResponse response = propertyService.batchCreateBills(request);
        return Result.success(response.getMessage(), response);
    }

    @Operation(summary = "批量延期账单", description = "将指定账单延期至指定时间。仅 ADMIN 可执行。")
    @Log(title = "即时账单", businessType = BusinessType.UPDATE)
    @PutMapping("/instant-bill/batch-defer")
    public Result<BatchOperationResponse> batchDefer(@Valid @RequestBody BatchDeferRequest request) {
        BatchOperationResponse response = propertyService.batchDeferBills(request);
        return Result.success(response.getMessage(), response);
    }

    @Operation(summary = "批量删除账单", description = "批量删除指定表计关联的消费扣费流水记录。仅 ADMIN 可执行。")
    @Log(title = "即时账单", businessType = BusinessType.DELETE)
    @DeleteMapping("/instant-bill/batch-delete")
    public Result<BatchOperationResponse> batchDelete(
            @Parameter(description = "表计ID列表", required = true)
            @RequestBody List<Long> meterIds) {
        BatchOperationResponse response = propertyService.batchDeleteBills(meterIds);
        return Result.success(response.getMessage(), response);
    }

    @Operation(summary = "批量导入账单", description = "通过 Excel 解析后的数据批量导入账单。仅 ADMIN 可执行。")
    @Log(title = "即时账单", businessType = BusinessType.IMPORT)
    @PostMapping("/instant-bill/batch-import")
    public Result<BatchOperationResponse> batchImport(@Valid @RequestBody BillImportRequest request) {
        BatchOperationResponse response = propertyService.batchImportBills(request);
        return Result.success(response.getMessage(), response);
    }

    @Operation(summary = "下载批量导入模板（Excel）", description = "下载账单批量导入的 Excel 模板文件，包含样例数据。")
    @GetMapping("/instant-bill/template/download")
    public void downloadTemplate(HttpServletResponse response) {
        propertyService.downloadImportTemplate(response);
    }

    // ──────────────────────── 缴费轨迹 ────────────────────────

    @Operation(summary = "缴费轨迹分页查询", description = "基于 t_account_flow 资金流水表查询缴费记录。"
            + "支持按商户、表计、交易类型、时间范围、关键字筛选。非 ADMIN 角色强制隔离。")
    @GetMapping("/billing-track/list")
    public Result<IPage<BillingTrackVO>> trackList(@Valid BillingTrackPageRequest request) {
        IPage<BillingTrackVO> page = propertyService.listBillingTracks(request);
        return Result.success(page);
    }

    @Operation(summary = "导出缴费轨迹（Excel）", description = "按筛选条件导出缴费轨迹为 Excel 文件。")
    @Log(title = "即时账单", businessType = BusinessType.EXPORT)
    @GetMapping("/billing-track/export")
    public void exportTrackList(@Valid BillingTrackPageRequest request, HttpServletResponse response) {
        propertyService.exportBillingTracks(response, request);
    }
}
