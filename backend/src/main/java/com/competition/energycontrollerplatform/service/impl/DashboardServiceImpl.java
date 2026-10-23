package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.energycontrollerplatform.dto.DashboardVO;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.mapper.AccountFlowMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import com.competition.energycontrollerplatform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘聚合统计服务实现。
 * <p>
 * 资产大盘与风险预警使用 MyBatis-Plus 条件查询，
 * 7天能耗趋势使用 JdbcTemplate 原生 SQL 以保证 GROUP BY 效率。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final MeterMapper meterMapper;
    private final CustomerMapper customerMapper;
    private final AccountFlowMapper accountFlowMapper;
    private final JdbcTemplate jdbcTemplate;

    /** 低余额告警阈值（元） */
    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("50.00");

    @Override
    @Cacheable(value = "dashboard",
               key = "T(com.competition.energycontrollerplatform.security.SecurityUserContext).isAdmin() " +
                     "? 'admin' " +
                     ": 'cust_' + T(com.competition.energycontrollerplatform.security.SecurityUserContext).getCurrentCustomerId()")
    public DashboardVO getStatistics() {
        DashboardVO vo = new DashboardVO();
        vo.setAssetOverview(buildAssetOverview());
        vo.setFinanceOverview(buildFinanceOverview());
        vo.setDailyTrends(buildDailyTrends());
        vo.setTodayHourlyTrends(buildTodayHourlyTrends());
        vo.setMonthlyDailyTrends(buildMonthlyDailyTrends());
        vo.setRiskAlerts(buildRiskAlerts());
        return vo;
    }

    // ────────────── 1. 实时资产大盘 ──────────────

    private DashboardVO.AssetOverview buildAssetOverview() {
        DashboardVO.AssetOverview overview = new DashboardVO.AssetOverview();

        // 权限隔离：非 ADMIN 只看自己商户的表计
        boolean isAdmin = SecurityUserContext.isAdmin();

        Long filterCustomerId = isAdmin ? null : SecurityUserContext.getCurrentCustomerId();

        LambdaQueryWrapper<com.competition.energycontrollerplatform.entity.Meter> wrapper =
                new LambdaQueryWrapper<>();
        wrapper.eq(com.competition.energycontrollerplatform.entity.Meter::getIsDeleted, 0);
        if (!isAdmin && filterCustomerId != null) {
            wrapper.eq(com.competition.energycontrollerplatform.entity.Meter::getCustomerId, filterCustomerId);
        }

        Long total = meterMapper.selectCount(wrapper);
        overview.setTotalMeters(total != null ? total : 0L);

        // 激活数 status=1
        wrapper.clear();
        wrapper.eq(com.competition.energycontrollerplatform.entity.Meter::getIsDeleted, 0)
               .eq(com.competition.energycontrollerplatform.entity.Meter::getStatus, 1);
        if (!isAdmin && filterCustomerId != null) {
            wrapper.eq(com.competition.energycontrollerplatform.entity.Meter::getCustomerId, filterCustomerId);
        }
        Long active = meterMapper.selectCount(wrapper);
        overview.setActiveMeters(active != null ? active : 0L);

        // 欠费停机数 status=2
        wrapper.clear();
        wrapper.eq(com.competition.energycontrollerplatform.entity.Meter::getIsDeleted, 0)
               .eq(com.competition.energycontrollerplatform.entity.Meter::getStatus, 2);
        if (!isAdmin && filterCustomerId != null) {
            wrapper.eq(com.competition.energycontrollerplatform.entity.Meter::getCustomerId, filterCustomerId);
        }
        Long overdue = meterMapper.selectCount(wrapper);
        overview.setOverdueMeters(overdue != null ? overdue : 0L);

        // 在线率：在线数 / 总设备数 × 100
        wrapper.clear();
        wrapper.eq(com.competition.energycontrollerplatform.entity.Meter::getIsDeleted, 0)
               .eq(com.competition.energycontrollerplatform.entity.Meter::getOnline, 1);
        if (!isAdmin && filterCustomerId != null) {
            wrapper.eq(com.competition.energycontrollerplatform.entity.Meter::getCustomerId, filterCustomerId);
        }
        Long online = meterMapper.selectCount(wrapper);
        if (total != null && total > 0) {
            overview.setOnlineRate(
                    BigDecimal.valueOf(online != null ? online : 0)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        } else {
            overview.setOnlineRate(BigDecimal.ZERO);
        }

        return overview;
    }

    // ────────────── 2. 财务资金大盘 ──────────────

    private DashboardVO.FinanceOverview buildFinanceOverview() {
        DashboardVO.FinanceOverview overview = new DashboardVO.FinanceOverview();

        boolean isAdmin = SecurityUserContext.isAdmin();
        Long filterCustomerId = isAdmin ? null : SecurityUserContext.getCurrentCustomerId();

        // 全盘商户总余额（非 ADMIN 仅统计当前商户）
        if (isAdmin || filterCustomerId == null) {
            BigDecimal totalBalance = customerMapper.sumTotalBalance();
            overview.setTotalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO);
        } else {
            // 非 ADMIN：仅统计当前商户余额
            BigDecimal totalBalance = customerMapper.selectById(filterCustomerId) != null
                    ? customerMapper.selectById(filterCustomerId).getBalance()
                    : BigDecimal.ZERO;
            overview.setTotalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO);
        }

        // 今日充值总额
        if (isAdmin || filterCustomerId == null) {
            BigDecimal todayRecharge = accountFlowMapper.sumTodayRecharge();
            overview.setTodayRechargeTotal(todayRecharge != null ? todayRecharge : BigDecimal.ZERO);
        } else {
            BigDecimal todayRecharge = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(change_amount), 0) FROM t_account_flow " +
                    "WHERE trade_type = 1 AND DATE(operate_time) = CURDATE() AND is_deleted = 0 AND customer_id = ?",
                    BigDecimal.class, filterCustomerId);
            overview.setTodayRechargeTotal(todayRecharge != null ? todayRecharge : BigDecimal.ZERO);
        }

        // 今日扣费总额
        if (isAdmin || filterCustomerId == null) {
            BigDecimal todayDebit = accountFlowMapper.sumTodayDebit();
            overview.setTodayDebitTotal(todayDebit != null ? todayDebit : BigDecimal.ZERO);
        } else {
            BigDecimal todayDebit = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(ABS(change_amount)), 0) FROM t_account_flow " +
                    "WHERE trade_type = 2 AND DATE(operate_time) = CURDATE() AND is_deleted = 0 AND customer_id = ?",
                    BigDecimal.class, filterCustomerId);
            overview.setTodayDebitTotal(todayDebit != null ? todayDebit : BigDecimal.ZERO);
        }

        return overview;
    }

    // ────────────── 3. 过去7天能耗趋势 ──────────────

    private List<DashboardVO.DailyTrend> buildDailyTrends() {
        boolean isAdmin = SecurityUserContext.isAdmin();
        Long filterCustomerId = isAdmin ? null : SecurityUserContext.getCurrentCustomerId();

        // 非 ADMIN 需 JOIN t_meter 过滤商户
        String sql;
        Object[] params = null;
        if (isAdmin || filterCustomerId == null) {
            sql = """
                    SELECT
                        DATE(r.create_time) AS record_date,
                        COALESCE(SUM(CASE WHEN m.meter_type = 1 THEN r.kwh ELSE 0 END), 0) AS total_electricity,
                        COALESCE(SUM(CASE WHEN m.meter_type = 0 THEN COALESCE(r.read_val, r.data_value0, r.kwh) ELSE 0 END), 0) AS total_water
                    FROM t_meter_reading r
                    INNER JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0
                    WHERE r.create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                      AND r.is_deleted = 0
                    GROUP BY DATE(r.create_time)
                    ORDER BY record_date ASC
                    """;
        } else {
            sql = """
                    SELECT
                        DATE(r.create_time) AS record_date,
                        COALESCE(SUM(CASE WHEN m.meter_type = 1 THEN r.kwh ELSE 0 END), 0) AS total_electricity,
                        COALESCE(SUM(CASE WHEN m.meter_type = 0 THEN COALESCE(r.read_val, r.data_value0, r.kwh) ELSE 0 END), 0) AS total_water
                    FROM t_meter_reading r
                    INNER JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0
                    WHERE r.create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                      AND r.is_deleted = 0
                      AND m.customer_id = ?
                    GROUP BY DATE(r.create_time)
                    ORDER BY record_date ASC
                    """;
            params = new Object[]{ filterCustomerId };
        }

        List<DashboardVO.DailyTrend> trends = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = params != null
                    ? jdbcTemplate.queryForList(sql, params)
                    : jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                DashboardVO.DailyTrend trend = new DashboardVO.DailyTrend();
                trend.setDate(row.get("record_date") != null ? row.get("record_date").toString() : "");
                trend.setTotalElectricity(toBigDecimal(row.get("total_electricity")));
                trend.setTotalWater(toBigDecimal(row.get("total_water")));
                trends.add(trend);
            }
        } catch (Exception e) {
            log.error("[Dashboard] 7天能耗趋势查询失败", e);
        }
        return trends;
    }

    // ────────────── 4. 今日24小时分时趋势 ──────────────

    private List<DashboardVO.HourlyTrend> buildTodayHourlyTrends() {
        boolean isAdmin = SecurityUserContext.isAdmin();
        Long filterCustomerId = isAdmin ? null : SecurityUserContext.getCurrentCustomerId();

        String sql;
        Object[] params = null;
        if (isAdmin || filterCustomerId == null) {
            sql = """
                    SELECT
                        HOUR(r.create_time) AS hour,
                        COALESCE(SUM(CASE WHEN m.meter_type = 1 THEN r.kwh ELSE 0 END), 0) AS total_electricity,
                        COALESCE(SUM(CASE WHEN m.meter_type = 0 THEN COALESCE(r.read_val, r.data_value0, r.kwh) ELSE 0 END), 0) AS total_water
                    FROM t_meter_reading r
                    INNER JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0
                    WHERE DATE(r.create_time) = CURDATE()
                      AND r.is_deleted = 0
                    GROUP BY HOUR(r.create_time)
                    ORDER BY hour ASC
                    """;
        } else {
            sql = """
                    SELECT
                        HOUR(r.create_time) AS hour,
                        COALESCE(SUM(CASE WHEN m.meter_type = 1 THEN r.kwh ELSE 0 END), 0) AS total_electricity,
                        COALESCE(SUM(CASE WHEN m.meter_type = 0 THEN COALESCE(r.read_val, r.data_value0, r.kwh) ELSE 0 END), 0) AS total_water
                    FROM t_meter_reading r
                    INNER JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0
                    WHERE DATE(r.create_time) = CURDATE()
                      AND r.is_deleted = 0
                      AND m.customer_id = ?
                    GROUP BY HOUR(r.create_time)
                    ORDER BY hour ASC
                    """;
            params = new Object[]{ filterCustomerId };
        }

        // 预填充 0–23 小时
        LinkedHashMap<Integer, DashboardVO.HourlyTrend> hourMap = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            DashboardVO.HourlyTrend t = new DashboardVO.HourlyTrend();
            t.setHour(h);
            t.setTotalElectricity(BigDecimal.ZERO);
            t.setTotalWater(BigDecimal.ZERO);
            hourMap.put(h, t);
        }

        try {
            List<Map<String, Object>> rows = params != null
                    ? jdbcTemplate.queryForList(sql, params)
                    : jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                Object hourObj = row.get("hour");
                if (hourObj != null) {
                    int hour = ((Number) hourObj).intValue();
                    DashboardVO.HourlyTrend t = hourMap.get(hour);
                    if (t != null) {
                        t.setTotalElectricity(toBigDecimal(row.get("total_electricity")));
                        t.setTotalWater(toBigDecimal(row.get("total_water")));
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 今日24小时趋势查询失败", e);
        }
        return new ArrayList<>(hourMap.values());
    }

    // ────────────── 5. 当月每日趋势 ──────────────

    private List<DashboardVO.MonthlyDailyTrend> buildMonthlyDailyTrends() {
        boolean isAdmin = SecurityUserContext.isAdmin();
        Long filterCustomerId = isAdmin ? null : SecurityUserContext.getCurrentCustomerId();

        YearMonth currentMonth = YearMonth.now();
        int daysInMonth = currentMonth.lengthOfMonth();

        String sql;
        Object[] params = null;
        if (isAdmin || filterCustomerId == null) {
            sql = """
                    SELECT
                        DATE(r.create_time) AS record_date,
                        COALESCE(SUM(CASE WHEN m.meter_type = 1 THEN r.kwh ELSE 0 END), 0) AS total_electricity,
                        COALESCE(SUM(CASE WHEN m.meter_type = 0 THEN COALESCE(r.read_val, r.data_value0, r.kwh) ELSE 0 END), 0) AS total_water
                    FROM t_meter_reading r
                    INNER JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0
                    WHERE YEAR(r.create_time) = YEAR(CURDATE())
                      AND MONTH(r.create_time) = MONTH(CURDATE())
                      AND r.is_deleted = 0
                    GROUP BY DATE(r.create_time)
                    ORDER BY record_date ASC
                    """;
        } else {
            sql = """
                    SELECT
                        DATE(r.create_time) AS record_date,
                        COALESCE(SUM(CASE WHEN m.meter_type = 1 THEN r.kwh ELSE 0 END), 0) AS total_electricity,
                        COALESCE(SUM(CASE WHEN m.meter_type = 0 THEN COALESCE(r.read_val, r.data_value0, r.kwh) ELSE 0 END), 0) AS total_water
                    FROM t_meter_reading r
                    INNER JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0
                    WHERE YEAR(r.create_time) = YEAR(CURDATE())
                      AND MONTH(r.create_time) = MONTH(CURDATE())
                      AND r.is_deleted = 0
                      AND m.customer_id = ?
                    GROUP BY DATE(r.create_time)
                    ORDER BY record_date ASC
                    """;
            params = new Object[]{ filterCustomerId };
        }

        // 预填充当月每一天
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LinkedHashMap<Integer, DashboardVO.MonthlyDailyTrend> dayMap = new LinkedHashMap<>();
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = currentMonth.atDay(d);
            DashboardVO.MonthlyDailyTrend t = new DashboardVO.MonthlyDailyTrend();
            t.setDate(date.format(fmt));
            t.setTotalElectricity(BigDecimal.ZERO);
            t.setTotalWater(BigDecimal.ZERO);
            dayMap.put(d, t);
        }

        try {
            List<Map<String, Object>> rows = params != null
                    ? jdbcTemplate.queryForList(sql, params)
                    : jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                Object dateObj = row.get("record_date");
                if (dateObj != null) {
                    LocalDate recordDate;
                    if (dateObj instanceof java.sql.Date sd) {
                        recordDate = sd.toLocalDate();
                    } else {
                        recordDate = LocalDate.parse(dateObj.toString(), fmt);
                    }
                    int day = recordDate.getDayOfMonth();
                    DashboardVO.MonthlyDailyTrend t = dayMap.get(day);
                    if (t != null) {
                        t.setTotalElectricity(toBigDecimal(row.get("total_electricity")));
                        t.setTotalWater(toBigDecimal(row.get("total_water")));
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Dashboard] 当月每日趋势查询失败", e);
        }
        return new ArrayList<>(dayMap.values());
    }

    // ────────────── 6. 风险预警雷达 ──────────────

    private List<DashboardVO.RiskAlert> buildRiskAlerts() {
        boolean isAdmin = SecurityUserContext.isAdmin();
        Long filterCustomerId = isAdmin ? null : SecurityUserContext.getCurrentCustomerId();

        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getIsDeleted, 0);

        // 权限隔离：非 ADMIN 只能看到自己商户的风险告警
        if (!isAdmin && filterCustomerId != null) {
            wrapper.eq(Customer::getId, filterCustomerId);
        }

        wrapper.lt(Customer::getBalance, LOW_BALANCE_THRESHOLD)
               .orderByAsc(Customer::getBalance)
               .last("LIMIT 5");

        List<Customer> customers = customerMapper.selectList(wrapper);
        List<DashboardVO.RiskAlert> alerts = new ArrayList<>();
        for (Customer c : customers) {
            DashboardVO.RiskAlert alert = new DashboardVO.RiskAlert();
            alert.setCustomerId(c.getId());
            alert.setCustomerName(c.getCustomerName());
            alert.setBalance(c.getBalance() != null ? c.getBalance() : BigDecimal.ZERO);
            alerts.add(alert);
        }
        return alerts;
    }

    // ────────────── 工具 ──────────────

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
