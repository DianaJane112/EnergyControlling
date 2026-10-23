package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.energycontrollerplatform.dto.OperationLogPageRequest;
import com.competition.energycontrollerplatform.dto.OperationLogVO;
import com.competition.energycontrollerplatform.entity.OperationLog;
import com.competition.energycontrollerplatform.mapper.OperationLogMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志查询服务实现。
 * <p>
 * 仅 ADMIN 角色可查询操作日志。日志写入由 LogAspect 切面自动完成。
 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogMapper operationLogMapper;

    @Override
    public IPage<OperationLogVO> page(OperationLogPageRequest request) {
        // 仅 ADMIN 可查询操作日志
        if (!SecurityUserContext.isAdmin()) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(
                    403, "仅管理员可查询操作日志");
        }

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 操作员 — 模糊匹配
        if (request.getOperatorName() != null && !request.getOperatorName().isBlank()) {
            wrapper.like(OperationLog::getOperatorName, request.getOperatorName().trim());
        }

        // 操作模块 — 精确匹配
        if (request.getModuleName() != null && !request.getModuleName().isBlank()) {
            wrapper.eq(OperationLog::getModuleName, request.getModuleName().trim());
        }

        // 操作类型 — 精确匹配
        if (request.getOperationType() != null && !request.getOperationType().isBlank()) {
            wrapper.eq(OperationLog::getOperationType, request.getOperationType().trim().toUpperCase());
        }

        // 操作描述 — 模糊匹配
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.like(OperationLog::getOperationDesc, request.getKeyword().trim());
        }

        // 表计ID筛选（模糊匹配 request_params JSON 中的 meterId）
        if (request.getMeterId() != null) {
            wrapper.like(OperationLog::getRequestParams, "\"meterId\":" + request.getMeterId());
        }

        // 时间范围
        if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
            LocalDateTime start = LocalDateTime.parse(request.getStartTime(), FORMATTER);
            wrapper.ge(OperationLog::getOperationTime, start);
        }
        if (request.getEndTime() != null && !request.getEndTime().isBlank()) {
            LocalDateTime end = LocalDateTime.parse(request.getEndTime(), FORMATTER);
            wrapper.le(OperationLog::getOperationTime, end);
        }

        // 默认按操作时间倒序
        wrapper.orderByDesc(OperationLog::getOperationTime);

        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        IPage<OperationLog> result = operationLogMapper.selectPage(page, wrapper);

        return result.convert(this::toVO);
    }

    @Override
    public IPage<OperationLogVO> pageByMeterId(Long meterId, int pageNum, int pageSize) {
        if (!SecurityUserContext.isAdmin()) {
            throw new com.competition.energycontrollerplatform.common.BusinessException(
                    403, "仅管理员可查询操作日志");
        }

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        // 通过在 request_params JSON 中模糊匹配 meterId
        wrapper.like(OperationLog::getRequestParams, "\"meterId\":" + meterId)
               .orderByDesc(OperationLog::getOperationTime);

        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        IPage<OperationLog> result = operationLogMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    // ──────────────────────── 私有方法 ────────────────────────

    private OperationLogVO toVO(OperationLog entity) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(entity.getId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setModuleName(entity.getModuleName());
        vo.setOperationType(entity.getOperationType());
        vo.setOperationDesc(entity.getOperationDesc());
        vo.setRequestParams(entity.getRequestParams());
        vo.setResponseResult(entity.getResponseResult());
        vo.setIpAddress(entity.getIpAddress());
        vo.setOperationTime(entity.getOperationTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
