package com.competition.energycontrollerplatform.controller;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.DashboardVO;
import com.competition.energycontrollerplatform.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页仪表盘 Controller
 */
@Tag(name = "系统 - 仪表盘", description = "首页数据聚合统计：资产大盘、财务概览、能耗趋势、风险预警")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取仪表盘全量聚合数据",
            description = "一次性返回资产大盘（总数/激活/停机/在线率）、财务概览（总余额/今日充值/今日扣费）、" +
                    "过去7天能耗趋势（每日电耗+水耗）、今日24小时分时趋势（每小时电耗+水耗）、" +
                    "当月每日趋势（1号起每日电耗+水耗，缺失日期补0）、风险预警（余额<50元的前5个商户）")
    @GetMapping("/statistics")
    public Result<DashboardVO> statistics() {
        DashboardVO vo = dashboardService.getStatistics();
        return Result.success(vo);
    }
}
