package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.dto.DashboardVO;

/**
 * 首页仪表盘聚合统计服务
 */
public interface DashboardService {

    /**
     * 获取仪表盘全量聚合数据
     */
    DashboardVO getStatistics();
}
