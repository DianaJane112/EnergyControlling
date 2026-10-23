package com.competition.energycontrollerplatform.miniapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 小程序用能分析专用 Mapper。
 * <p>
 * 数据来源：tb_meter_daily_report（统一水电日报表）、tb_meter_monthly_report（月报表）。
 * 预聚合表无数据时降级查询 t_meter_reading 原始读数。
 */
@Mapper
public interface MiniAppEnergyAnalysisMapper {

    /**
     * 按商户 + 月份查询每日用电量（从日报表），按日期升序。
     *
     * @param customerId 商户ID
     * @param monthStart 月初日期
     * @param monthEnd   月末日期
     * @return [{stat_date, dailyUsage}] 列表
     */
    @Select("SELECT stat_date AS statDate, COALESCE(SUM(total_usage), 0) AS dailyUsage "
            + "FROM tb_meter_daily_report "
            + "WHERE merchant_id = #{customerId} "
            + "AND meter_type = 'ELECTRIC' "
            + "AND stat_date >= #{monthStart} "
            + "AND stat_date <= #{monthEnd} "
            + "GROUP BY stat_date "
            + "ORDER BY stat_date ASC")
    List<Map<String, Object>> sumDailyElectricByMonth(@Param("customerId") Long customerId,
                                                       @Param("monthStart") LocalDate monthStart,
                                                       @Param("monthEnd") LocalDate monthEnd);

    /**
     * 按商户 + 年份查询每月用电量（从月报表），按月份升序。
     *
     * @param customerId 商户ID
     * @param year       统计年份（yyyy）
     * @return [{stat_month, monthlyUsage}] 列表
     */
    @Select("SELECT stat_month AS statMonth, COALESCE(SUM(total_usage), 0) AS monthlyUsage "
            + "FROM tb_meter_monthly_report "
            + "WHERE merchant_id = #{customerId} "
            + "AND meter_type = 'ELECTRIC' "
            + "AND stat_year = #{year} "
            + "GROUP BY stat_month "
            + "ORDER BY stat_month ASC")
    List<Map<String, Object>> sumMonthlyElectricByYear(@Param("customerId") Long customerId,
                                                        @Param("year") String year);

    // ──────────────── 降级查询（直接查 t_meter_reading） ────────────────

    /**
     * [降级] 从原始读数表按日聚合用电量。
     * 当 tb_meter_daily_report 无数据时使用。
     */
    @Select("SELECT DATE(r.create_time) AS statDate, COALESCE(SUM(r.kwh), 0) AS dailyUsage "
            + "FROM t_meter_reading r "
            + "INNER JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0 "
            + "WHERE m.customer_id = #{customerId} "
            + "AND m.meter_type = 1 "
            + "AND r.create_time >= #{monthStart} "
            + "AND r.create_time < #{nextMonthStart} "
            + "AND r.is_deleted = 0 "
            + "GROUP BY DATE(r.create_time) "
            + "ORDER BY DATE(r.create_time) ASC")
    List<Map<String, Object>> sumDailyElectricFromReadings(@Param("customerId") Long customerId,
                                                            @Param("monthStart") LocalDate monthStart,
                                                            @Param("nextMonthStart") LocalDate nextMonthStart);

    /**
     * [降级] 从原始读数表按月聚合用电量。
     * 当 tb_meter_monthly_report 无数据时使用。
     */
    @Select("SELECT DATE_FORMAT(r.create_time, '%Y-%m') AS statMonth, "
            + "COALESCE(SUM(r.kwh), 0) AS monthlyUsage "
            + "FROM t_meter_reading r "
            + "INNER JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0 "
            + "WHERE m.customer_id = #{customerId} "
            + "AND m.meter_type = 1 "
            + "AND YEAR(r.create_time) = #{yearInt} "
            + "AND r.is_deleted = 0 "
            + "GROUP BY DATE_FORMAT(r.create_time, '%Y-%m') "
            + "ORDER BY statMonth ASC")
    List<Map<String, Object>> sumMonthlyElectricFromReadings(@Param("customerId") Long customerId,
                                                              @Param("yearInt") int yearInt);

    @Select("SELECT stat_date AS statDate, COALESCE(SUM(total_usage), 0) AS dailyUsage "
            + "FROM tb_meter_daily_report WHERE (meter_id = #{meterId} OR meter_id IN (SELECT child_meter_id "
            + "FROM t_shadow_meter WHERE parent_meter_id = #{meterId} AND status = 1 AND is_deleted = 0)) "
            + "AND meter_type = 'ELECTRIC' "
            + "AND stat_date >= #{monthStart} AND stat_date <= #{monthEnd} "
            + "GROUP BY stat_date ORDER BY stat_date ASC")
    List<Map<String, Object>> sumDailyElectricByMeter(@Param("meterId") Long meterId,
                                                       @Param("monthStart") LocalDate monthStart,
                                                       @Param("monthEnd") LocalDate monthEnd);

    @Select("SELECT stat_month AS statMonth, COALESCE(SUM(total_usage), 0) AS monthlyUsage "
            + "FROM tb_meter_monthly_report WHERE (meter_id = #{meterId} OR meter_id IN (SELECT child_meter_id "
            + "FROM t_shadow_meter WHERE parent_meter_id = #{meterId} AND status = 1 AND is_deleted = 0)) "
            + "AND meter_type = 'ELECTRIC' "
            + "AND stat_year = #{year} GROUP BY stat_month ORDER BY stat_month ASC")
    List<Map<String, Object>> sumMonthlyElectricByMeter(@Param("meterId") Long meterId,
                                                         @Param("year") String year);

    @Select("SELECT DATE(r.create_time) AS statDate, COALESCE(SUM(r.kwh), 0) AS dailyUsage "
            + "FROM t_meter_reading r WHERE (r.meter_id = #{meterId} OR r.meter_id IN (SELECT child_meter_id "
            + "FROM t_shadow_meter WHERE parent_meter_id = #{meterId} AND status = 1 AND is_deleted = 0)) "
            + "AND r.create_time >= #{monthStart} "
            + "AND r.create_time < #{nextMonthStart} AND r.is_deleted = 0 "
            + "GROUP BY DATE(r.create_time) ORDER BY DATE(r.create_time) ASC")
    List<Map<String, Object>> sumDailyElectricReadingByMeter(@Param("meterId") Long meterId,
                                                              @Param("monthStart") LocalDate monthStart,
                                                              @Param("nextMonthStart") LocalDate nextMonthStart);

    @Select("SELECT DATE_FORMAT(r.create_time, '%Y-%m') AS statMonth, COALESCE(SUM(r.kwh), 0) AS monthlyUsage "
            + "FROM t_meter_reading r WHERE (r.meter_id = #{meterId} OR r.meter_id IN (SELECT child_meter_id "
            + "FROM t_shadow_meter WHERE parent_meter_id = #{meterId} AND status = 1 AND is_deleted = 0)) "
            + "AND YEAR(r.create_time) = #{yearInt} "
            + "AND r.is_deleted = 0 GROUP BY DATE_FORMAT(r.create_time, '%Y-%m') ORDER BY statMonth ASC")
    List<Map<String, Object>> sumMonthlyElectricReadingByMeter(@Param("meterId") Long meterId,
                                                                @Param("yearInt") int yearInt);
}
