package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.BillCategoryRequest;
import com.competition.energycontrollerplatform.entity.BillCategory;
import com.competition.energycontrollerplatform.service.BillCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账单分类 Controller。
 * <p>提供账单分类的新增、删除、编辑、分页查询功能。</p>
 */
@Tag(name = "即时账单 - 账单分类", description = "账单分类的新增、删除、编辑、查询接口")
@RestController
@RequestMapping("/api/v1/bill-category")
@RequiredArgsConstructor
@Validated
public class BillCategoryController {

    private final BillCategoryService billCategoryService;

    @Operation(summary = "分页查询账单分类列表", description = "支持按关键字（分类名称/编码）模糊搜索")
    @GetMapping("/page")
    public Result<IPage<BillCategory>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "关键字（分类名称/编码模糊匹配）") @RequestParam(required = false) String keyword) {
        IPage<BillCategory> page = billCategoryService.listPage(pageNum, pageSize, keyword);
        return Result.success(page);
    }

    @Operation(summary = "获取所有启用的账单分类（下拉列表）")
    @GetMapping("/enabled")
    public Result<List<BillCategory>> enabledList() {
        List<BillCategory> list = billCategoryService.listEnabled();
        return Result.success(list);
    }

    @Operation(summary = "账单分类详情")
    @GetMapping("/{id}")
    public Result<BillCategory> detail(@Parameter(description = "分类ID") @PathVariable Long id) {
        BillCategory category = billCategoryService.getById(id);
        return Result.success(category);
    }

    @Operation(summary = "新增账单分类")
    @Log(title = "账单分类", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<BillCategory> create(@Valid @RequestBody BillCategoryRequest request) {
        BillCategory category = billCategoryService.create(request);
        return Result.success(category);
    }

    @Operation(summary = "编辑账单分类")
    @Log(title = "账单分类", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<BillCategory> update(@Parameter(description = "分类ID") @PathVariable Long id,
                                       @Valid @RequestBody BillCategoryRequest request) {
        BillCategory category = billCategoryService.update(id, request);
        return Result.success(category);
    }

    @Operation(summary = "删除账单分类（逻辑删除）")
    @Log(title = "账单分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        billCategoryService.delete(id);
        return Result.success("删除成功");
    }
}
