package com.competition.energycontrollerplatform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.dto.OperationLogPageRequest;
import com.competition.energycontrollerplatform.dto.OperationLogVO;
import com.competition.energycontrollerplatform.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志 Controller。
 * <p>
 * 提供操作日志的分页查询与高级检索。
 * 日志写入由 {@code LogAspect} 切面自动切入所有标注了 {@code @Log} 的 Controller 方法。
 */
@Tag(name = "系统 - 操作日志", description = "操作日志分页查询、按操作员/模块/时间筛选")
@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
@Validated
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "操作日志分页查询", description = "支持按操作员（模糊）、操作模块（精确）、操作类型（精确）、"
            + "操作描述关键字（模糊）、时间范围进行高级检索。"
            + "<p>仅 ADMIN 角色可查询操作日志。"
            + "<p><b>操作类型枚举：</b>QUERY-查询, CREATE-新增, UPDATE-修改, DELETE-删除, EXPORT-导出")
    @GetMapping("/page")
    public Result<IPage<OperationLogVO>> page(@Valid OperationLogPageRequest request) {
        IPage<OperationLogVO> page = operationLogService.page(request);
        return Result.success(page);
    }
}
