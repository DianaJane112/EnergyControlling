package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.common.*;
import com.competition.energycontrollerplatform.entity.BillingSegment;
import com.competition.energycontrollerplatform.mapper.BillingSegmentMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class DimensionStatisticsController {
    private final BillingSegmentMapper mapper;

    @GetMapping("/dimension-summary")
    public Result<List<Map<String,Object>>> summary(
            @RequestParam(defaultValue="MONTH") String periodType,
            @RequestParam(defaultValue="PROJECT") String dimension,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required=false) Long projectId, @RequestParam(required=false) Long areaGroupId,
            @RequestParam(required=false) Long categoryGroupId, @RequestParam(required=false) Long virtualMeterId) {
        periodType = periodType.toUpperCase(Locale.ROOT);
        dimension = dimension.toUpperCase(Locale.ROOT);
        if (!Set.of("DAY","MONTH","YEAR").contains(periodType)) throw BusinessException.of(400,"统计周期非法");
        if (!Set.of("PROJECT","AREA","CATEGORY","VIRTUAL_ACCOUNT").contains(dimension)) throw BusinessException.of(400,"统计维度非法");
        Long customerId = SecurityUserContext.isAdmin() ? null : SecurityUserContext.getCurrentCustomerId();
        return Result.success(mapper.dimensionSummary(periodType, dimension, startDate, endDate, customerId,
                projectId, areaGroupId, categoryGroupId, virtualMeterId));
    }

    @GetMapping("/dimension-detail")
    public Result<List<BillingSegment>> detail(
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required=false) Long projectId, @RequestParam(required=false) Long areaGroupId,
            @RequestParam(required=false) Long categoryGroupId, @RequestParam(required=false) Long virtualMeterId) {
        Long customerId = SecurityUserContext.isAdmin() ? null : SecurityUserContext.getCurrentCustomerId();
        LambdaQueryWrapper<BillingSegment> q = new LambdaQueryWrapper<BillingSegment>()
                .ge(startDate!=null, BillingSegment::getBillingDate, startDate)
                .le(endDate!=null, BillingSegment::getBillingDate, endDate)
                .eq(customerId!=null, BillingSegment::getCustomerId, customerId)
                .eq(projectId!=null, BillingSegment::getProjectId, projectId)
                .eq(areaGroupId!=null, BillingSegment::getAreaGroupId, areaGroupId)
                .eq(categoryGroupId!=null, BillingSegment::getCategoryGroupId, categoryGroupId)
                .eq(virtualMeterId!=null, BillingSegment::getVirtualMeterId, virtualMeterId)
                .orderByDesc(BillingSegment::getPeriodEnd).last("LIMIT 1000");
        return Result.success(mapper.selectList(q));
    }
}
