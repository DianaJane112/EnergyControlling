package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.MeterPageRequest;
import com.competition.energycontrollerplatform.dto.MeterResponse;
import com.competition.energycontrollerplatform.service.MeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 新版前端财务页面仍使用的历史表计查询路径。 */
@Tag(name = "兼容接口 - 表计信息")
@RestController
@RequestMapping("/api/v1/info")
@RequiredArgsConstructor
@Validated
public class LegacyMeterInfoController {

    private final MeterService meterService;

    @Operation(summary = "兼容旧路径的表计分页查询")
    @GetMapping("/dbPage")
    public Result<IPage<MeterResponse>> dbPage(@Valid MeterPageRequest request) {
        return Result.success(meterService.listPage(request));
    }

    @Operation(summary = "按表号查询表计详情")
    @GetMapping("/dbDetail")
    public Result<MeterResponse> dbDetail(@RequestParam @NotBlank String dbNo) {
        MeterPageRequest request = new MeterPageRequest();
        request.setPageNum(1);
        request.setPageSize(1);
        request.setDbNo(dbNo.trim());
        IPage<MeterResponse> page = meterService.listPage(request);
        if (page.getRecords().isEmpty()) {
            throw BusinessException.of(404, "表计不存在或无权访问");
        }
        return Result.success(page.getRecords().get(0));
    }
}
