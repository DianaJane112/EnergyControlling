package com.competition.energycontrollerplatform.service;

import java.math.BigDecimal;

/**
 * 能耗消费计算与自动扣费服务
 */
public interface EnergyConsumptionService {

    /**
     * 对指定表计执行一次「读数差 → 计费 → 扣款」结算闭环。
     *
     * @param meterId      表计ID
     * @param defaultPrice 默认单价（元/kWh 或 元/吨），当无费率配置时使用
     * @return 扣除金额（BigDecimal），若读数无变化则返回 BigDecimal.ZERO
     */
    BigDecimal calculateAndDebit(Long meterId, BigDecimal defaultPrice);
}
