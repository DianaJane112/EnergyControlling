package com.competition.energycontrollerplatform.miniapp.controller;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.miniapp.dto.DashboardVO;
import com.competition.energycontrollerplatform.miniapp.security.MiniappSecurityContext;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 小程序 - 控制台仪表盘控制器
 */
@RestController
@RequestMapping("/api/v1/miniapp")
@RequiredArgsConstructor
@Tag(name = "小程序-仪表盘", description = "控制台仪表盘数据")
public class MiniAppDashboardController {

    private final MiniAppDashboardService miniAppDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘数据", description = "当前电表/水表余额、预计可用天数、用电/用水统计")
    public Result<DashboardVO> dashboard(@RequestParam(required = false) Long meterId) {
        Long userId = MiniappSecurityContext.getCurrentUserId();
        Long customerId = MiniappSecurityContext.getCurrentCustomerId();
        DashboardVO vo = miniAppDashboardService.getDashboard(userId, customerId, meterId);
        return Result.success(vo);
    }
}
