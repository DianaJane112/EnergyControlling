package com.competition.energycontrollerplatform.miniapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.Map;

/**
 * 小程序仪表盘专用 Mapper。
 * <p>
 * 数据来源：tb_meter_daily_report（统一水电日报表）和 tb_meter_monthly_report（月报表）。
 */
@Mapper
public interface MiniAppDashboardMapper {

    /**
     * 按商户 + 表计类型 + 日期范围汇总用量。
     */
    @Select("<script>"
            + "SELECT COALESCE(SUM(total_usage), 0) AS totalUsage "
            + "FROM tb_meter_daily_report "
            + "WHERE merchant_id = #{customerId} "
            + "AND meter_type = #{meterType} "
            + "<if test='startDate != null'> AND stat_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND stat_date &lt;= #{endDate} </if>"
            + "</script>")
    Map<String, Object> sumDailyUsage(@Param("customerId") Long customerId,
                                       @Param("meterType") String meterType,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * 按商户 + 表计类型 + 月份汇总用量。
     */
    @Select("<script>"
            + "SELECT COALESCE(SUM(total_usage), 0) AS totalUsage "
            + "FROM tb_meter_monthly_report "
            + "WHERE merchant_id = #{customerId} "
            + "AND meter_type = #{meterType} "
            + "<if test='statMonth != null and statMonth != \"\"'> AND stat_month = #{statMonth} </if>"
            + "</script>")
    Map<String, Object> sumMonthlyUsage(@Param("customerId") Long customerId,
                                         @Param("meterType") String meterType,
                                         @Param("statMonth") String statMonth);

    @Select("<script>SELECT COALESCE(SUM(total_usage), 0) AS totalUsage FROM tb_meter_daily_report "
            + "WHERE (meter_id = #{meterId} OR meter_id IN (SELECT child_meter_id FROM t_shadow_meter "
            + "WHERE parent_meter_id = #{meterId} AND status = 1 AND is_deleted = 0 "
            + "AND effective_date &lt;= CURRENT_DATE AND (expire_date IS NULL OR expire_date &gt;= CURRENT_DATE))) "
            + "AND meter_type = #{meterType} "
            + "<if test='startDate != null'> AND stat_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND stat_date &lt;= #{endDate} </if></script>")
    Map<String, Object> sumDailyUsageByMeter(@Param("meterId") Long meterId,
                                              @Param("meterType") String meterType,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Select("SELECT COALESCE(SUM(total_usage), 0) AS totalUsage FROM tb_meter_monthly_report "
            + "WHERE (meter_id = #{meterId} OR meter_id IN (SELECT child_meter_id FROM t_shadow_meter "
            + "WHERE parent_meter_id = #{meterId} AND status = 1 AND is_deleted = 0 "
            + "AND effective_date &lt;= CURRENT_DATE AND (expire_date IS NULL OR expire_date &gt;= CURRENT_DATE))) "
            + "AND meter_type = #{meterType} AND stat_month = #{statMonth}")
    Map<String, Object> sumMonthlyUsageByMeter(@Param("meterId") Long meterId,
                                                @Param("meterType") String meterType,
                                                @Param("statMonth") String statMonth);
}
