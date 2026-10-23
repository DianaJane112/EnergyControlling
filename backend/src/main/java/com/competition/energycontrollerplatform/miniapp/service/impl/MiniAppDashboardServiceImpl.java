package com.competition.energycontrollerplatform.miniapp.service.impl;

import com.competition.energycontrollerplatform.miniapp.dto.DashboardVO;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.miniapp.mapper.MiniAppDashboardMapper;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppDashboardService;
import com.competition.energycontrollerplatform.miniapp.service.MiniAppMeterAccessService;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * 小程序仪表盘服务实现。
 * <p>
 * 数据来源：tb_meter_daily_report（统一水电日报表）和 tb_meter_monthly_report（月报表）。
 * 余额统一使用 t_customer.balance。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniAppDashboardServiceImpl implements MiniAppDashboardService {

    private final CustomerMapper customerMapper;
    private final MiniAppDashboardMapper miniAppDashboardMapper;
    private final MiniAppMeterAccessService meterAccessService;

    @Override
    public DashboardVO getDashboard(Long userId, Long customerId, Long meterId) {
        Meter selectedMeter = meterAccessService.requireViewable(userId, customerId, meterId);
        Long balanceCustomerId = selectedMeter != null ? selectedMeter.getCustomerId() : customerId;
        if (balanceCustomerId == null) {
            throw new BusinessException(403, "请先由管理员分配可查看的虚拟表");
        }
        // 1. 查询商户信息
        Customer customer = customerMapper.selectById(balanceCustomerId);
        if (customer == null) {
            return DashboardVO.builder().build();
        }

        BigDecimal balance = customer.getBalance() != null ? customer.getBalance() : BigDecimal.ZERO;

        // 2. 计算日期
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDate sevenDaysAgo = today.minusDays(7);
        String lastMonth = today.minusMonths(1).toString().substring(0, 7); // yyyy-MM

        // 3. 查询今日用电量
        BigDecimal todayElectric = sumDailyUsage(customerId, meterId, "ELECTRIC", today, today);

        // 4. 查询近30日用电量
        BigDecimal last30DaysElectric = sumDailyUsage(customerId, meterId, "ELECTRIC", thirtyDaysAgo, yesterday);

        // 5. 查询昨日用水量
        BigDecimal yesterdayWater = sumDailyUsage(customerId, meterId, "WATER", yesterday, yesterday);

        // 6. 查询上月用水量
        BigDecimal lastMonthWater = sumMonthlyUsage(customerId, meterId, "WATER", lastMonth);

        // 7. 计算预计可用天数（基于近7天日均用量）
        BigDecimal last7DaysElectric = sumDailyUsage(customerId, meterId, "ELECTRIC", sevenDaysAgo, yesterday);
        BigDecimal avgDailyElectric = last7DaysElectric.divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP);

        BigDecimal last7DaysWater = sumDailyUsage(customerId, meterId, "WATER", sevenDaysAgo, yesterday);
        BigDecimal avgDailyWater = last7DaysWater.divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP);

        int electricDays = 999;
        if (avgDailyElectric.compareTo(BigDecimal.ZERO) > 0) {
            electricDays = balance.divide(avgDailyElectric, 0, RoundingMode.DOWN).intValue();
            if (electricDays > 999) electricDays = 999;
        }

        int waterDays = 999;
        if (avgDailyWater.compareTo(BigDecimal.ZERO) > 0) {
            waterDays = balance.divide(avgDailyWater, 0, RoundingMode.DOWN).intValue();
            if (waterDays > 999) waterDays = 999;
        }

        return DashboardVO.builder()
                .customerName(customer.getCustomerName())
                .electricBalance(balance)
                .electricEstimatedDays(electricDays)
                .waterBalance(balance)
                .waterEstimatedDays(waterDays)
                .todayElectricUsage(todayElectric)
                .last30DaysElectricUsage(last30DaysElectric)
                .yesterdayWaterUsage(yesterdayWater)
                .lastMonthWaterUsage(lastMonthWater)
                .build();
    }

    /**
     * 查询某商户某类型表计在日期范围内的用量总和（从日报表）。
     */
    private BigDecimal sumDailyUsage(Long customerId, Long meterId, String meterType, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = meterId != null
                ? miniAppDashboardMapper.sumDailyUsageByMeter(meterId, meterType, startDate, endDate)
                : miniAppDashboardMapper.sumDailyUsage(customerId, meterType, startDate, endDate);
        if (result == null || result.get("totalUsage") == null) {
            return BigDecimal.ZERO;
        }
        Object val = result.get("totalUsage");
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        return new BigDecimal(val.toString());
    }

    /**
     * 查询某商户某类型表计在某月的用量总和（从月报表）。
     */
    private BigDecimal sumMonthlyUsage(Long customerId, Long meterId, String meterType, String statMonth) {
        Map<String, Object> result = meterId != null
                ? miniAppDashboardMapper.sumMonthlyUsageByMeter(meterId, meterType, statMonth)
                : miniAppDashboardMapper.sumMonthlyUsage(customerId, meterType, statMonth);
        if (result == null || result.get("totalUsage") == null) {
            return BigDecimal.ZERO;
        }
        Object val = result.get("totalUsage");
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        return new BigDecimal(val.toString());
    }
}
