package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.entity.MeterOperationTrace;
import com.competition.energycontrollerplatform.mapper.MeterOperationTraceMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/meter-operation-traces")
@RequiredArgsConstructor
public class MeterOperationTraceController {
  private final MeterOperationTraceMapper mapper;

  @GetMapping("/page")
  public Result<IPage<MeterOperationTrace>> page(@RequestParam(required=false) String objectType,
      @RequestParam(required=false) Long objectId, @RequestParam(required=false) Long customerId,
      @RequestParam(required=false) String eventType, @RequestParam(required=false) String operatorName,
      @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
      @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
      @RequestParam(defaultValue="1") long pageNum, @RequestParam(defaultValue="20") long pageSize) {
    if (!SecurityUserContext.isAdmin()) {
      customerId = SecurityUserContext.getCurrentCustomerId();
      if (customerId == null) throw BusinessException.of(403, "无权限查看操作轨迹");
    }
    LambdaQueryWrapper<MeterOperationTrace> q = new LambdaQueryWrapper<MeterOperationTrace>()
        .eq(MeterOperationTrace::getIsDeleted, 0).eq(objectType != null && !objectType.isBlank(), MeterOperationTrace::getObjectType, objectType)
        .eq(objectId != null, MeterOperationTrace::getObjectId, objectId).eq(customerId != null, MeterOperationTrace::getCustomerId, customerId)
        .eq(eventType != null && !eventType.isBlank(), MeterOperationTrace::getEventType, eventType)
        .like(operatorName != null && !operatorName.isBlank(), MeterOperationTrace::getOperatorName, operatorName)
        .ge(startTime != null, MeterOperationTrace::getOperationTime, startTime).le(endTime != null, MeterOperationTrace::getOperationTime, endTime)
        .orderByDesc(MeterOperationTrace::getOperationTime);
    return Result.success(mapper.selectPage(new Page<>(pageNum, pageSize), q));
  }

  @GetMapping("/{id}")
  public Result<MeterOperationTrace> detail(@PathVariable Long id) {
    MeterOperationTrace trace = mapper.selectById(id);
    if (trace == null) throw BusinessException.of(404, "操作轨迹不存在");
    if (!SecurityUserContext.isAdmin() && !java.util.Objects.equals(trace.getCustomerId(), SecurityUserContext.getCurrentCustomerId()))
      throw BusinessException.of(403, "无权查看该操作轨迹");
    return Result.success(trace);
  }
}
