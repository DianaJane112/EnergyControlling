package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.PriceStrategyRequest;
import com.competition.energycontrollerplatform.entity.PriceStrategy;
import com.competition.energycontrollerplatform.service.PriceStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 费率时段策略 Controller。
 * <p>
 * 支持固定电价、分时电价（尖峰平谷）、阶梯电价三种策略类型的 CRUD 管理。
 */
@Tag(name = "系统 - 费率策略", description = "费率时段策略（固定/分时/阶梯电价）的增删改查接口")
@RestController
@RequestMapping("/api/v1/price-strategy")
@RequiredArgsConstructor
@Validated
public class PriceStrategyController {

    private final PriceStrategyService priceStrategyService;

    @Operation(summary = "策略分页列表", description = "按关键词模糊搜索策略名称或编码")
    @GetMapping("/page")
    public Result<IPage<PriceStrategy>> page(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "搜索关键词（策略名称/编码）") @RequestParam(required = false) String keyword,
            @Parameter(description = "表计类型：0-水表，1-电表") @RequestParam(required = false) Integer meterType) {
        IPage<PriceStrategy> page = priceStrategyService.listPage(pageNum, pageSize, keyword, meterType);
        return Result.success(page);
    }

    @Operation(summary = "策略详情")
    @GetMapping("/{id}")
    public Result<PriceStrategy> detail(@Parameter(description = "策略ID") @PathVariable Long id) {
        PriceStrategy strategy = priceStrategyService.getById(id);
        return Result.success(strategy);
    }

    @Operation(summary = "新增策略")
    @Log(title = "费率策略", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<PriceStrategy> create(@Valid @RequestBody PriceStrategyRequest request) {
        PriceStrategy strategy = priceStrategyService.create(request);
        return Result.success(strategy);
    }

    @Operation(summary = "编辑策略")
    @Log(title = "费率策略", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public Result<PriceStrategy> update(@Parameter(description = "策略ID") @PathVariable Long id,
                                        @Valid @RequestBody PriceStrategyRequest request) {
        PriceStrategy strategy = priceStrategyService.update(id, request);
        return Result.success(strategy);
    }

    @Operation(summary = "删除策略（逻辑删除）")
    @Log(title = "费率策略", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "策略ID") @PathVariable Long id) {
        priceStrategyService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "启停策略", description = "启用（status=1）或停用（status=0）指定费率策略")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "策略ID") @PathVariable Long id,
                                     @Parameter(description = "状态：1-启用, 0-停用") @RequestParam Integer status) {
        priceStrategyService.updateStatus(id, status);
        return Result.success(status == 1 ? "策略已启用" : "策略已停用");
    }
}
