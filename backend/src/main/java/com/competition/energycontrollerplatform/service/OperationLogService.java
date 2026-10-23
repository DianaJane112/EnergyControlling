package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.OperationLogPageRequest;
import com.competition.energycontrollerplatform.dto.OperationLogVO;

/**
 * 操作日志查询服务接口。
 * <p>
 * 提供操作日志的分页查询与高级检索。
 * 日志写入由 {@code LogAspect} 切面自动完成，本服务仅负责查询。
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志。
     * <p>
     * 支持按操作员（模糊）、操作模块（精确）、操作类型（精确）、
     * 操作描述关键字（模糊）、时间范围、表计ID筛选。
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<OperationLogVO> page(OperationLogPageRequest request);

    /**
     * 按表计ID查询操作日志分页。
     * <p>
     * 通过在 request_params JSON 字段中模糊匹配 meterId 实现筛选。
     * 用于「更多操作」面板中查看某台表计的操作历史。
     *
     * @param meterId  表计ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 操作日志分页
     */
    IPage<OperationLogVO> pageByMeterId(Long meterId, int pageNum, int pageSize);
}
