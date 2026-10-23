package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.common.annotation.BusinessType;
import com.competition.energycontrollerplatform.common.annotation.Log;
import com.competition.energycontrollerplatform.dto.WaterMeterVendorRequest;
import com.competition.energycontrollerplatform.entity.WaterMeterVendor;
import com.competition.energycontrollerplatform.service.WaterMeterVendorService;
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

@Tag(name = "水表管理 - 水表对接", description = "水表厂商 API 对接配置管理")
@RestController
@RequestMapping("/api/v1/water-meter-vendor")
@RequiredArgsConstructor
@Validated
public class WaterMeterVendorController {

    private final WaterMeterVendorService waterMeterVendorService;

    @Operation(summary = "水表厂商分页列表", description = "按厂商名、API 地址、厂商类型模糊查询")
    @GetMapping("/page")
    public Result<IPage<WaterMeterVendor>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        return Result.success(waterMeterVendorService.listPage(pageNum, pageSize, keyword));
    }

    @Operation(summary = "水表厂商详情")
    @GetMapping("/{id}")
    public Result<WaterMeterVendor> detail(@Parameter(description = "水表厂商ID") @PathVariable Long id) {
        return Result.success(waterMeterVendorService.getById(id));
    }

    @Operation(summary = "新增水表厂商")
    @Log(title = "水表对接", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<WaterMeterVendor> create(@Valid @RequestBody WaterMeterVendorRequest request) {
        return Result.success(waterMeterVendorService.create(request));
    }

    @Operation(summary = "启停水表厂商")
    @Log(title = "水表对接", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "水表厂商ID") @PathVariable Long id,
                                     @Parameter(description = "状态：1-启用, 0-停用") @RequestParam Integer status) {
        waterMeterVendorService.updateStatus(id, status);
        return Result.success(status == 1 ? "水表厂商已启用" : "水表厂商已停用");
    }
}
