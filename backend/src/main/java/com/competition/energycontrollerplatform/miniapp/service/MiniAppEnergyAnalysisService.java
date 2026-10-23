package com.competition.energycontrollerplatform.miniapp.service;

import com.competition.energycontrollerplatform.miniapp.dto.DailyTrendVO;
import com.competition.energycontrollerplatform.miniapp.dto.MonthlyTrendVO;

import java.util.List;

/**
 * 小程序用能分析服务接口。
 */
public interface MiniAppEnergyAnalysisService {

    /**
     * 获取指定月份的每日用电趋势。
     *
     * @param customerId 商户ID
     * @param month      月份（yyyy-MM），默认当月
     * @return 每日用电量数据点列表（按日期升序，缺失日期填充0）
     */
    List<DailyTrendVO> getDailyElectricTrend(Long userId, Long customerId, Long meterId, String month);

    /**
     * 获取指定年份的每月用电趋势。
     *
     * @param customerId 商户ID
     * @param year       年份（yyyy），默认当年
     * @return 每月用电量数据点列表（按月份升序，缺失月份填充0）
     */
    List<MonthlyTrendVO> getMonthlyElectricTrend(Long userId, Long customerId, Long meterId, String year);
}
