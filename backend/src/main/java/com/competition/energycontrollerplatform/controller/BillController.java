package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.BillCreateRequest;
import com.competition.energycontrollerplatform.dto.BillPageRequest;
import com.competition.energycontrollerplatform.dto.BatchOperationResponse;
import com.competition.energycontrollerplatform.entity.Bill;
import com.competition.energycontrollerplatform.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 账单记录 Controller。
 * <p>提供账单记录的增删改查、批量删除功能。账单查询字段与前端的15列表头对齐。</p>
 */
@Tag(name = "即时账单 - 账单记录管理", description = "账单记录的新增、编辑、删除、分页查询、批量删除接口")
@RestController
@RequestMapping("/api/v1/bill")
@RequiredArgsConstructor
@Validated
public class BillController {

    private final BillService billService;

    // ── 分页列表 ──────────────────────────────────────────────────────────

    @Operation(summary = "账单记录分页列表", description = "支持按商户名、表号、虚拟表号、分类、支付状态、时间范围筛选")
    @GetMapping("/page")
    public Result<IPage<Bill>> page(@Valid BillPageRequest request) {
        IPage<Bill> result = billService.listPage(request);
        return Result.success(result);
    }

    // ── 详情 ──────────────────────────────────────────────────────────────

    @Operation(summary = "账单详情")
    @GetMapping("/{id}")
    public Result<Bill> detail(@Parameter(description = "账单ID") @PathVariable Long id) {
        Bill bill = billService.getById(id);
        if (bill == null) {
            return Result.error(404, "账单不存在或已删除");
        }
        return Result.success(bill);
    }

    // ── 新增 ──────────────────────────────────────────────────────────────

    @Operation(summary = "新增账单")
    @Log(title = "账单记录", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<Bill> create(@Valid @RequestBody BillCreateRequest request) {
        Bill bill = billService.create(request);
        return Result.success("新增成功", bill);
    }

    // ── 批量新增 ──────────────────────────────────────────────────────────

    @Operation(summary = "批量新增账单", description = "一次创建多条账单记录。仅 ADMIN 可执行。")
    @Log(title = "账单记录", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public Result<BatchOperationResponse> batchCreate(@Valid @RequestBody List<BillCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Result.error(400, "请求列表不能为空");
        }
        BatchOperationResponse resp = billService.batchCreate(requests);
        return Result.success(resp.getMessage(), resp);
    }

    // ── 编辑 ──────────────────────────────────────────────────────────────

    @Operation(summary = "编辑账单")
    @Log(title = "账单记录", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<Bill> update(@Parameter(description = "账单ID") @PathVariable Long id,
                               @Valid @RequestBody BillCreateRequest request) {
        Bill bill = billService.update(id, request);
        if (bill == null) {
            return Result.error(404, "账单不存在或已删除");
        }
        return Result.success("编辑成功", bill);
    }

    // ── 删除（单条）──────────────────────────────────────────────────────

    @Operation(summary = "删除账单（逻辑删除）")
    @Log(title = "账单记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "账单ID") @PathVariable Long id) {
        billService.delete(id);
        return Result.success("删除成功");
    }

    // ── 批量删除 ──────────────────────────────────────────────────────────

    @Operation(summary = "批量删除账单", description = "同时兼容两种 JSON 格式：{\"ids\":[1,2,3]} 或 [1,2,3]")
    @PostMapping("/batch-delete")
    public Result<BatchOperationResponse> batchDelete(HttpServletRequest httpRequest) {
        try {
            String body = httpRequest.getReader().lines()
                    .collect(java.util.stream.Collectors.joining());
            BatchOperationResponse resp = billService.batchDelete(body);
            return Result.success(resp.getMessage(), resp);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(400, "请求体解析失败：" + e.getMessage());
        }
    }

    // ── 导出 ──────────────────────────────────────────────────────────────

    @Operation(summary = "导出账单列表（Excel）", description = "按当前筛选条件导出账单数据为 Excel 文件。")
    @GetMapping("/export")
    public void export(BillPageRequest request, HttpServletResponse response) throws IOException {
        billService.exportList(response, request);
    }

    // ── 模板下载 ──────────────────────────────────────────────────────────

    @Operation(summary = "下载批量导入模板（Excel）", description = "下载账单批量导入的 Excel 模板，含表头行和样例数据")
    @GetMapping("/template/download")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        billService.downloadTemplate(response);
    }
}
