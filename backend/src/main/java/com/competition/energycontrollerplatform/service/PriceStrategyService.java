package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.PriceStrategyRequest;
import com.competition.energycontrollerplatform.entity.PriceStrategy;

/**
 * 费率时段策略服务接口。
 */
public interface PriceStrategyService {

    /** 分页查询 */
    IPage<PriceStrategy> listPage(int pageNum, int pageSize, String keyword, Integer meterType);

    /** 查询详情 */
    PriceStrategy getById(Long id);

    /** 新增策略 */
    PriceStrategy create(PriceStrategyRequest request);

    /** 编辑策略 */
    PriceStrategy update(Long id, PriceStrategyRequest request);

    /** 删除策略（逻辑删除） */
    void delete(Long id);

    /** 启停策略 */
    void updateStatus(Long id, Integer status);
}
