package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.CurvePointVO;
import com.competition.energycontrollerplatform.dto.MeterReadingPageRequest;
import com.competition.energycontrollerplatform.dto.MeterReadingVO;
import com.competition.energycontrollerplatform.service.MeterReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表计实时读数 Controller
 */
@Tag(name = "电能/水能管理 - 表计读数", description = "表计历史读数查询、实时曲线数据接口")
@RestController
@RequestMapping("/api/v1/meter-reading")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @Operation(summary = "分页历史读数查询", description = "按表计ID + 时间范围分页查询历史能耗数据")
    @GetMapping("/page")
    public Result<IPage<MeterReadingVO>> page(@Valid MeterReadingPageRequest request) {
        IPage<MeterReadingVO> page = meterReadingService.listPage(request);
        return Result.success(page);
    }

    @Operation(summary = "实时曲线数据查询", description = "返回最近24小时的电压/电流/功率/电量趋势数据，供ECharts折线图使用")
    @GetMapping("/curve")
    public Result<List<CurvePointVO>> curve(
            @Parameter(description = "表计ID", required = true, example = "1")
            @RequestParam Long meterId) {
        List<CurvePointVO> curve = meterReadingService.getCurve(meterId);
        return Result.success(curve);
    }
}
