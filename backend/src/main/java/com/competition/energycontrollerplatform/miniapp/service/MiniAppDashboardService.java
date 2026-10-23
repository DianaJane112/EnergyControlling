package com.competition.energycontrollerplatform.miniapp.service;

import com.competition.energycontrollerplatform.miniapp.dto.DashboardVO;

/**
 * 小程序仪表盘服务接口
 */
public interface MiniAppDashboardService {

    /**
     * 获取仪表盘数据。
     * <p>
     * 根据当前商户ID，汇总电表和水表的：
     * - 当前余额和预计可用天数
     * - 今日/近30日用电量
     * - 昨日/上月用水量
     *
     * @param customerId 商户ID
     * @return 仪表盘数据
     */
    DashboardVO getDashboard(Long userId, Long customerId, Long meterId);
}
