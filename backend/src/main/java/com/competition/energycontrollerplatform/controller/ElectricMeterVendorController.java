package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.ElectricMeterVendorRequest;
import com.competition.energycontrollerplatform.entity.ElectricMeterVendor;
import com.competition.energycontrollerplatform.service.ElectricMeterVendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "电表管理 - 电表对接", description = "电表厂商 API 对接配置管理")
@RestController
@RequestMapping("/api/v1/electric-meter-vendor")
@RequiredArgsConstructor
@Validated
public class ElectricMeterVendorController {

    private final ElectricMeterVendorService electricMeterVendorService;

    @Operation(summary = "电表厂商分页列表", description = "按电表厂商名称、API地址或AppId模糊查询")
    @GetMapping("/page")
    public Result<IPage<ElectricMeterVendor>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return Result.success(electricMeterVendorService.listPage(pageNum, pageSize, keyword));
    }

    @Operation(summary = "电表厂商详情")
    @GetMapping("/{id}")
    public Result<ElectricMeterVendor> detail(@Parameter(description = "电表厂商ID") @PathVariable Long id) {
        return Result.success(electricMeterVendorService.getById(id));
    }

    @Operation(summary = "新增电表厂商")
    @Log(title = "电表对接", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<ElectricMeterVendor> create(@Valid @RequestBody ElectricMeterVendorRequest request) {
        return Result.success(electricMeterVendorService.create(request));
    }

    @Operation(summary = "启停电表厂商")
    @Log(title = "电表对接", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "电表厂商ID") @PathVariable Long id,
                                     @Parameter(description = "状态：1-启用, 0-停用") @RequestParam Integer status) {
        electricMeterVendorService.updateStatus(id, status);
        return Result.success(status == 1 ? "电表厂商已启用" : "电表厂商已停用");
    }
}
