package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.ReportDailyElectricity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用电量日报表 Mapper。
 */
@Mapper
public interface ReportDailyElectricityMapper extends BaseMapper<ReportDailyElectricity> {

    /**
     * 按日期范围 + 维度汇总用电量。
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     * @return [{period, totalConsumed, meterCount}]
     */
    @Select("<script>"
            + "SELECT report_date AS period, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY report_date "
            + "ORDER BY report_date DESC"
            + "</script>")
    List<Map<String, Object>> sumByDate(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("customerId") Long customerId,
                                         @Param("groupId") Long groupId);

    /**
     * 按月汇总用电量。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(report_date, '%Y-%m') AS period, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY DATE_FORMAT(report_date, '%Y-%m') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByMonth(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("customerId") Long customerId,
                                          @Param("groupId") Long groupId);

    /**
     * 客户综合报表：总充值、总用量、总费用、当前余额。
     */
    @Select("<script>"
            + "SELECT c.id AS customerId, c.customer_name AS customerName, c.balance AS currentBalance, "
            + "       COALESCE(r.totalConsumed, 0) AS totalConsumed, "
            + "       COALESCE(p.totalRecharge, 0) AS totalRecharge, "
            + "       COALESCE(p.totalCharge, 0) AS totalCharge "
            + "FROM t_customer c "
            + "LEFT JOIN ("
            + "    SELECT customer_id, SUM(electricity_consumed) AS totalConsumed "
            + "    FROM t_report_daily_electricity "
            + "    WHERE 1=1 "
            + "    <if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "    <if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "    GROUP BY customer_id"
            + ") r ON c.id = r.customer_id "
            + "LEFT JOIN ("
            + "    SELECT customer_id, "
            + "           COALESCE(SUM(CASE WHEN trade_type = 1 THEN change_amount ELSE 0 END), 0) AS totalRecharge, "
            + "           COALESCE(SUM(CASE WHEN trade_type = 2 THEN ABS(change_amount) ELSE 0 END), 0) AS totalCharge "
            + "    FROM t_account_flow "
            + "    WHERE is_deleted = 0 "
            + "    <if test='startDate != null'> AND operate_time &gt;= #{startDate} </if>"
            + "    <if test='endDate != null'> AND operate_time &lt;= #{endDate} </if>"
            + "    GROUP BY customer_id"
            + ") p ON c.id = p.customer_id "
            + "WHERE c.is_deleted = 0 "
            + "<if test='customerId != null'> AND c.id = #{customerId} </if>"
            + "ORDER BY c.id"
            + "</script>")
    List<Map<String, Object>> customerComprehensiveReport(@Param("startDate") String startDate,
                                                          @Param("endDate") String endDate,
                                                          @Param("customerId") Long customerId);

    /**
     * 按表计ID列表 + 日期范围汇总各表用电量（用于按历史用量比例分摊）。
     *
     * @param meterIds  表计ID列表
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return [{meterId, totalConsumed}]
     */
    @Select("<script>"
            + "SELECT meter_id AS meterId, COALESCE(SUM(electricity_consumed), 0) AS totalConsumed "
            + "FROM t_report_daily_electricity "
            + "WHERE meter_id IN "
            + "<foreach collection='meterIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "GROUP BY meter_id"
            + "</script>")
    List<Map<String, Object>> sumByMeterIds(@Param("meterIds") List<Long> meterIds,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    // ──────────────── 年度聚合（§五.7） ────────────────

    /**
     * 按年汇总用电量。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(report_date, '%Y') AS period, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY DATE_FORMAT(report_date, '%Y') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByYear(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("customerId") Long customerId,
                                         @Param("groupId") Long groupId);

    // ──────────────── 复费率分时聚合（§五.4/5） ────────────────

    /**
     * 按日聚合复费率分时用电量（尖/峰/平/谷分列）。
     */
    @Select("<script>"
            + "SELECT report_date AS reportDate, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalConsumed, "
            + "       COALESCE(SUM(peak_consumed), 0) AS peakConsumed, "
            + "       COALESCE(SUM(busy_consumed), 0) AS busyConsumed, "
            + "       COALESCE(SUM(normal_consumed), 0) AS normalConsumed, "
            + "       COALESCE(SUM(idle_consumed), 0) AS idleConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY report_date "
            + "ORDER BY report_date DESC"
            + "</script>")
    List<Map<String, Object>> sumTimeOfUseByDate(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("customerId") Long customerId,
                                                   @Param("groupId") Long groupId);

    /**
     * 按月聚合复费率分时用电量（尖/峰/平/谷分列）。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(report_date, '%Y-%m') AS period, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalConsumed, "
            + "       COALESCE(SUM(peak_consumed), 0) AS peakConsumed, "
            + "       COALESCE(SUM(busy_consumed), 0) AS busyConsumed, "
            + "       COALESCE(SUM(normal_consumed), 0) AS normalConsumed, "
            + "       COALESCE(SUM(idle_consumed), 0) AS idleConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY DATE_FORMAT(report_date, '%Y-%m') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumTimeOfUseByMonth(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("customerId") Long customerId,
                                                    @Param("groupId") Long groupId);

    /**
     * 按日查询复费率分时用电量明细（不聚合，返回每条 meter x date 记录）。
     * <p>用于复费率计费电量日报表明细表格，包含商户号/表号/分时电费字段。</p>
     */
    @Select("<script>"
            + "SELECT r.report_date AS reportDate, "
            + "       r.meter_id AS meterId, "
            + "       COALESCE(NULLIF(m.steal_no, ''), m.db_no) AS deviceNo, "
            + "       c.customer_code AS customerCode, "
            + "       c.customer_name AS customerName, "
            + "       COALESCE(r.electricity_consumed, 0) AS totalConsumed, "
            + "       COALESCE(r.charge_amount, 0) AS totalCharge, "
            + "       COALESCE(r.peak_consumed, 0) AS peakConsumed, "
            + "       COALESCE(r.peak_charge_amount, 0) AS peakCharge, "
            + "       COALESCE(r.busy_consumed, 0) AS busyConsumed, "
            + "       COALESCE(r.busy_charge_amount, 0) AS busyCharge, "
            + "       COALESCE(r.normal_consumed, 0) AS normalConsumed, "
            + "       COALESCE(r.normal_charge_amount, 0) AS normalCharge, "
            + "       COALESCE(r.idle_consumed, 0) AS idleConsumed, "
            + "       COALESCE(r.idle_charge_amount, 0) AS idleCharge "
            + "FROM t_report_daily_electricity r "
            + "LEFT JOIN t_meter m ON r.meter_id = m.id "
            + "LEFT JOIN t_customer c ON r.customer_id = c.id "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND r.report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND r.report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND r.customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND r.group_id = #{groupId} </if>"
            + "ORDER BY r.report_date DESC, r.meter_id ASC"
            + "</script>")
    List<Map<String, Object>> sumTimeOfUseByDateDetail(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate,
                                                        @Param("customerId") Long customerId,
                                                        @Param("groupId") Long groupId);

    // ──────────────── 计费金额聚合（§五.8/9/10） ────────────────

    /**
     * 按日聚合计费金额。
     */
    @Select("<script>"
            + "SELECT report_date AS period, "
            + "       COALESCE(SUM(charge_amount), 0) AS totalChargeAmount, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY report_date "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumBillingByDate(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("customerId") Long customerId,
                                                 @Param("groupId") Long groupId);

    /**
     * 按月聚合计费金额。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(report_date, '%Y-%m') AS period, "
            + "       COALESCE(SUM(charge_amount), 0) AS totalChargeAmount, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY DATE_FORMAT(report_date, '%Y-%m') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumBillingByMonth(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("customerId") Long customerId,
                                                  @Param("groupId") Long groupId);

    /**
     * 按年聚合计费金额。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(report_date, '%Y') AS period, "
            + "       COALESCE(SUM(charge_amount), 0) AS totalChargeAmount, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY DATE_FORMAT(report_date, '%Y') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumBillingByYear(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("customerId") Long customerId,
                                                 @Param("groupId") Long groupId);

    // ──────────────── 用电负荷查询（§五.12） ────────────────

    /**
     * 按日查询用电负荷（最大/平均/最小日用电量 + 负荷率）。
     * <p>对每日各表计总用电量做 max/avg/min 聚合，反映每日峰值负荷和负荷均衡度。</p>
     */
    @Select("<script>"
            + "SELECT report_date AS period, "
            + "       COALESCE(MAX(daily_total), 0) AS maxLoad, "
            + "       COALESCE(AVG(daily_total), 0) AS averageLoad, "
            + "       COALESCE(MIN(daily_total), 0) AS minLoad, "
            + "       COUNT(*) AS dayCount, "
            + "       COALESCE(SUM(daily_total), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM ("
            + "    SELECT report_date, meter_id, SUM(electricity_consumed) AS daily_total "
            + "    FROM t_report_daily_electricity "
            + "    WHERE 1=1 "
            + "    <if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "    <if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "    <if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "    <if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "    GROUP BY report_date, meter_id"
            + ") sub "
            + "GROUP BY report_date "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> electricLoadByDate(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("customerId") Long customerId,
                                                  @Param("groupId") Long groupId);

    /**
     * 按月查询用电负荷（最大/平均/最小日用电量 + 负荷率）。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(report_date, '%Y-%m') AS period, "
            + "       COALESCE(MAX(daily_total), 0) AS maxLoad, "
            + "       COALESCE(AVG(daily_total), 0) AS averageLoad, "
            + "       COALESCE(MIN(daily_total), 0) AS minLoad, "
            + "       COUNT(*) AS dayCount, "
            + "       COALESCE(SUM(daily_total), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM ("
            + "    SELECT report_date, meter_id, SUM(electricity_consumed) AS daily_total "
            + "    FROM t_report_daily_electricity "
            + "    WHERE 1=1 "
            + "    <if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "    <if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "    <if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "    <if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "    GROUP BY report_date, meter_id"
            + ") sub "
            + "GROUP BY DATE_FORMAT(report_date, '%Y-%m') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> electricLoadByMonth(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("customerId") Long customerId,
                                                   @Param("groupId") Long groupId);

    // ──────────────── 用电负荷曲线（§五.12 曲线图） ────────────────

    /**
     * 查询每日用电负荷曲线数据（按日聚合总用电量 + 分时用电量）。
     * <p>
     * 与 electricLoadByDate 的区别：本方法返回每日总用电量及各时段分量，
     * 按日期升序排列，专用于前端曲线图渲染。
     * electricLoadByDate 返回的是 max/avg/min 聚合统计，用于负荷率表格展示。
     * </p>
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     * @return [{date, totalLoad, peakLoad, busyLoad, normalLoad, idleLoad, meterCount}]
     */
    @Select("<script>"
            + "SELECT report_date AS date, "
            + "       COALESCE(SUM(electricity_consumed), 0) AS totalLoad, "
            + "       COALESCE(SUM(peak_consumed), 0) AS peakLoad, "
            + "       COALESCE(SUM(busy_consumed), 0) AS busyLoad, "
            + "       COALESCE(SUM(normal_consumed), 0) AS normalLoad, "
            + "       COALESCE(SUM(idle_consumed), 0) AS idleLoad, "
            + "       COUNT(DISTINCT meter_id) AS meterCount "
            + "FROM t_report_daily_electricity "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND group_id = #{groupId} </if>"
            + "GROUP BY report_date "
            + "ORDER BY report_date ASC"
            + "</script>")
    List<Map<String, Object>> electricLoadCurve(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("customerId") Long customerId,
                                                 @Param("groupId") Long groupId);

    // ──────────────── 用电量月报表明细（按表计+每日用量） ────────────────

    /**
     * 查询某月内各表计每日用电量明细。
     * <p>
     * JOIN t_meter / t_customer / t_meter_group 获取维度信息，
     * 返回原始行数据，由 Service 层按 meterId 聚合为 Map。
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     * @return [{meterId, deviceNo, meterType, groupName, merchantName, reportDate, dailyUsage}]
     */
    @Select("<script>"
            + "SELECT m.id AS meterId, "
            + "       COALESCE(NULLIF(m.steal_no, ''), m.db_no) AS deviceNo, "
            + "       CASE WHEN m.meter_type = 1 THEN 'ELECTRIC' WHEN m.meter_type = 0 THEN 'WATER' ELSE 'UNKNOWN' END AS meterType, "
            + "       COALESCE(g.group_name, '') AS groupName, "
            + "       COALESCE(c.customer_name, '') AS customerName, "
            + "       r.report_date AS reportDate, "
            + "       COALESCE(r.electricity_consumed, 0) AS dailyUsage "
            + "FROM t_report_daily_electricity r "
            + "JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0 "
            + "LEFT JOIN t_customer c ON m.customer_id = c.id "
            + "LEFT JOIN t_meter_group g ON m.group_id = g.id "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND r.report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND r.report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND r.customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND r.group_id = #{groupId} </if>"
            + "ORDER BY m.id, r.report_date"
            + "</script>")
    List<Map<String, Object>> monthlyDetailByMeter(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("customerId") Long customerId,
                                                    @Param("groupId") Long groupId);

    // ──────────────── 计费电量汇总（按表计维度） ────────────────

    /**
     * 按表计维度聚合计费金额与用电量，含平均电价。
     * <p>
     * SUM charge_amount / electricity_consumed，同时 JOIN t_meter/t_customer 获取维度信息。
     * 虚拟表信息由 Service 层批量补充。
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     * @return [{meterId, deviceNo, customerName, totalChargeAmount, totalConsumed}]
     */
    @Select("<script>"
            + "SELECT r.meter_id AS meterId, "
            + "       COALESCE(NULLIF(m.steal_no, ''), m.db_no) AS deviceNo, "
            + "       COALESCE(c.customer_name, '') AS customerName, "
            + "       COALESCE(SUM(r.charge_amount), 0) AS totalChargeAmount, "
            + "       COALESCE(SUM(r.electricity_consumed), 0) AS totalConsumed "
            + "FROM t_report_daily_electricity r "
            + "JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0 "
            + "LEFT JOIN t_customer c ON m.customer_id = c.id "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND r.report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND r.report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND r.customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND r.group_id = #{groupId} </if>"
            + "GROUP BY r.meter_id, m.steal_no, m.db_no, c.customer_name "
            + "ORDER BY totalChargeAmount DESC"
            + "</script>")
    List<Map<String, Object>> billingByMeter(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("customerId") Long customerId,
                                              @Param("groupId") Long groupId);

    // ──────────────── 计费电量月报表明细（按表计+每日计费） ────────────────

    /**
     * 查询当月内各表计每日计费电量与金额明细。
     * <p>
     * 按表计维度展开每日计费数据（electricity_consumed + charge_amount），
     * JOIN 获取设备类型、分组、商户等维度信息，
     * 由 Service 层按 meterId 聚合为 BillingMonthlyMeterVO。
     *
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     * @return [{meterId, deviceNo, deviceType, groupName, merchantName, reportDate, dailyKwh, dailyAmt}]
     */
    @Select("<script>"
            + "SELECT r.meter_id AS meterId, "
            + "       COALESCE(NULLIF(m.steal_no, ''), m.db_no) AS deviceNo, "
            + "       CASE m.bjlx "
            + "         WHEN 1 THEN '租户' "
            + "         WHEN 2 THEN '总表' "
            + "         WHEN 3 THEN '公共' "
            + "         WHEN 4 THEN '其他' "
            + "         ELSE '未知' "
            + "       END AS deviceType, "
            + "       COALESCE(g.group_name, '') AS groupName, "
            + "       COALESCE(c.customer_name, '') AS merchantName, "
            + "       r.report_date AS reportDate, "
            + "       COALESCE(r.electricity_consumed, 0) AS dailyKwh, "
            + "       COALESCE(r.charge_amount, 0) AS dailyAmt "
            + "FROM t_report_daily_electricity r "
            + "JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0 "
            + "LEFT JOIN t_customer c ON m.customer_id = c.id "
            + "LEFT JOIN t_meter_group g ON m.group_id = g.id "
            + "WHERE 1=1 "
            + "<if test='startDate != null'> AND r.report_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND r.report_date &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND r.customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND r.group_id = #{groupId} </if>"
            + "ORDER BY m.bjlx, r.meter_id, r.report_date"
            + "</script>")
    List<Map<String, Object>> billingMonthlyDetailByMeter(@Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate,
                                                           @Param("customerId") Long customerId,
                                                           @Param("groupId") Long groupId);

    // ──────────────── 已删除表计月报（§五.3） ────────────────

    /**
     * 查询已删除电表的月度用电汇总。
     * <p>
     * 从 t_report_daily_electricity 按 (meter_id, 月份) 实时聚合，
     * LEFT JOIN t_meter（含已删除记录）获取已删除表计及维度信息。
     * 不再依赖 tb_meter_monthly_report 预聚合表。
     */
    @Select("<script>"
            + "SELECT d.statMonth, "
            + "       m.steal_no AS deviceNo, "
            + "       m.id AS meterId, "
            + "       m.meter_type AS meterType, "
            + "       c.customer_name AS merchantName, "
            + "       p.project_name AS projectName, "
            + "       g.group_name AS groupName, "
            + "       COALESCE(d.totalUsage, 0) AS totalUsage, "
            + "       COALESCE(d.peakUsage, 0) AS peakUsage, "
            + "       COALESCE(d.busyUsage, 0) AS busyUsage, "
            + "       COALESCE(d.normalUsage, 0) AS normalUsage, "
            + "       COALESCE(d.idleUsage, 0) AS idleUsage, "
            + "       m.update_time AS deletedTime, "
            + "       d.dailyCount, "
            + "       'DELETED_METER' AS dataSource, "
            + "       vm.steal_no AS virtualMeterNo, "
            + "       vc.customer_name AS virtualMerchantName "
            + "FROM ("
            + "    SELECT rde.meter_id, "
            + "           rde.customer_id, "
            + "           rde.group_id, "
            + "           DATE_FORMAT(rde.report_date, '%Y-%m') AS statMonth, "
            + "           COALESCE(SUM(rde.electricity_consumed), 0) AS totalUsage, "
            + "           COALESCE(SUM(rde.peak_consumed), 0) AS peakUsage, "
            + "           COALESCE(SUM(rde.busy_consumed), 0) AS busyUsage, "
            + "           COALESCE(SUM(rde.normal_consumed), 0) AS normalUsage, "
            + "           COALESCE(SUM(rde.idle_consumed), 0) AS idleUsage, "
            + "           COUNT(DISTINCT rde.report_date) AS dailyCount "
            + "    FROM t_report_daily_electricity rde "
            + "    WHERE 1=1 "
            + "    <if test='statMonth != null and statMonth != \"\"'> AND DATE_FORMAT(rde.report_date, '%Y-%m') = #{statMonth} </if>"
            + "    <if test='customerId != null'> AND rde.customer_id = #{customerId} </if>"
            + "    <if test='groupId != null'> AND rde.group_id = #{groupId} </if>"
            + "    GROUP BY rde.meter_id, rde.customer_id, rde.group_id, DATE_FORMAT(rde.report_date, '%Y-%m')"
            + ") d "
            + "JOIN t_meter m ON d.meter_id = m.id AND m.is_deleted = 1 "
            + "LEFT JOIN t_customer c ON m.customer_id = c.id "
            + "LEFT JOIN t_project p ON m.pro_id = p.id "
            + "LEFT JOIN t_meter_group g ON m.group_id = g.id "
            + "LEFT JOIN t_shadow_meter sm ON sm.parent_meter_id = m.id AND sm.is_deleted = 0 AND sm.status = 1 "
            + "LEFT JOIN t_meter vm ON sm.child_meter_id = vm.id "
            + "LEFT JOIN t_customer vc ON vm.customer_id = vc.id "
            + "ORDER BY d.statMonth DESC, m.update_time DESC"
            + "</script>")
    List<Map<String, Object>> deletedMeterMonthlyReport(@Param("statMonth") String statMonth,
                                                        @Param("customerId") Long customerId,
                                                        @Param("groupId") Long groupId);

    // ──────────────── 年度用电量汇总（按表计×12月明细） ────────────────

    /**
     * 查询指定年份内各表计的月度用电量明细（1月~12月分列 + 小计）。
     * <p>
     * 按 meter_id + 年份分组，用条件聚合生成12列月度用电量。
     * 包含表计维度信息（表号、商户名、表计类型）。
     * 虚拟表信息由 Service 层批量补充。
     *
     * @param year       统计年份（yyyy）
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     * @return [{meterId, deviceNo, customerName, meterType, month01..month12, subtotal}]
     */
    @Select("<script>"
            + "SELECT r.meter_id AS meterId, "
            + "       COALESCE(NULLIF(m.steal_no, ''), m.db_no) AS deviceNo, "
            + "       COALESCE(c.customer_name, '') AS customerName, "
            + "       CASE m.bjlx "
            + "         WHEN 1 THEN '租户' "
            + "         WHEN 2 THEN '总表' "
            + "         WHEN 3 THEN '公共' "
            + "         WHEN 4 THEN '其他' "
            + "         ELSE '未知' "
            + "       END AS meterType, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  1 THEN r.electricity_consumed ELSE 0 END), 0) AS month01, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  2 THEN r.electricity_consumed ELSE 0 END), 0) AS month02, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  3 THEN r.electricity_consumed ELSE 0 END), 0) AS month03, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  4 THEN r.electricity_consumed ELSE 0 END), 0) AS month04, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  5 THEN r.electricity_consumed ELSE 0 END), 0) AS month05, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  6 THEN r.electricity_consumed ELSE 0 END), 0) AS month06, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  7 THEN r.electricity_consumed ELSE 0 END), 0) AS month07, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  8 THEN r.electricity_consumed ELSE 0 END), 0) AS month08, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) =  9 THEN r.electricity_consumed ELSE 0 END), 0) AS month09, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) = 10 THEN r.electricity_consumed ELSE 0 END), 0) AS month10, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) = 11 THEN r.electricity_consumed ELSE 0 END), 0) AS month11, "
            + "       COALESCE(SUM(CASE WHEN MONTH(r.report_date) = 12 THEN r.electricity_consumed ELSE 0 END), 0) AS month12, "
            + "       COALESCE(SUM(r.electricity_consumed), 0) AS subtotal "
            + "FROM t_report_daily_electricity r "
            + "JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0 "
            + "LEFT JOIN t_customer c ON m.customer_id = c.id "
            + "WHERE YEAR(r.report_date) = #{year} "
            + "<if test='customerId != null'> AND r.customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND r.group_id = #{groupId} </if>"
            + "GROUP BY r.meter_id, m.steal_no, m.db_no, c.customer_name, m.bjlx "
            + "ORDER BY m.bjlx, r.meter_id"
            + "</script>")
    List<Map<String, Object>> yearlyDetailByMeter(@Param("year") String year,
                                                   @Param("customerId") Long customerId,
                                                   @Param("groupId") Long groupId);

    // ──────────────── 计费电量年报明细（按表计+年度/本月汇总宽表） ────────────────

    /**
     * 查询计费电量年报明细（按表计×年度/本月汇总宽表）。
     * <p>
     * 每个表计一行：年度计费金额/用电量合计 + 本月计费金额/用电量合计 + 余额。
     * </p>
     * <p>
     * <b>余额取法（修复人工充值导致的虚增问题）：</b>
     * <ul>
     *   <li>{@code previousBalance}：优先从 t_account_flow 取统计月初第一条流水的
     *       balance_before（精确月初余额），兜底用 c.balance + monthCharge 反推</li>
     *   <li>{@code currentBalance}：标量子查询直接从 t_customer 取，避免 GROUP BY
     *       meter_id 导致同一商户的余额在每台表计行中复制 N 份</li>
     * </ul>
     * 虚拟表信息由 Service 层批量补充。
     *
     * @param year       统计年份（yyyy，如 "2026"）
     * @param statMonth  统计月份（yyyy-MM，如 "2026-06"）
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     * @return 按表计维度汇总的计费年报数据
     */
    @Select("<script>"
            + "SELECT r.meter_id AS meterId, "
            + "       COALESCE(NULLIF(m.steal_no, ''), m.db_no) AS deviceNo, "
            + "       COALESCE(c.customer_name, '') AS customerName, "
            + "       COALESCE(g.group_name, '') AS groupName, "
            + "       COALESCE(SUM(CASE WHEN YEAR(r.report_date) = #{year} "
            + "                THEN r.charge_amount ELSE 0 END), 0) AS yearAmount, "
            + "       COALESCE(SUM(CASE WHEN YEAR(r.report_date) = #{year} "
            + "                THEN r.electricity_consumed ELSE 0 END), 0) AS yearUsage, "
            + "       COALESCE(SUM(CASE WHEN DATE_FORMAT(r.report_date, '%Y-%m') = #{statMonth} "
            + "                THEN r.charge_amount ELSE 0 END), 0) AS monthAmount, "
            + "       COALESCE(SUM(CASE WHEN DATE_FORMAT(r.report_date, '%Y-%m') = #{statMonth} "
            + "                THEN r.electricity_consumed ELSE 0 END), 0) AS monthUsage, "
            // previousBalance：优先从 t_account_flow 取月初第一条流水的 balance_before
            // 兜底用 c.balance + monthCharge（无流水记录时的近似值）
            + "       COALESCE("
            + "         (SELECT af.balance_before FROM t_account_flow af "
            + "          WHERE af.meter_id = r.meter_id "
            + "            AND af.operate_time >= CONCAT(#{statMonth}, '-01') "
            + "            AND af.is_deleted = 0 "
            + "          ORDER BY af.operate_time ASC LIMIT 1), "
            + "         COALESCE(c.balance, 0) "
            + "           + COALESCE(SUM(CASE WHEN DATE_FORMAT(r.report_date, '%Y-%m') = #{statMonth} "
            + "                  THEN r.charge_amount ELSE 0 END), 0)"
            + "       ) AS previousBalance, "
            // currentBalance：标量子查询按 customer_id 取，避免 GROUP BY meter_id 复制 N 份
            + "       COALESCE((SELECT cust.balance FROM t_customer cust "
            + "                  WHERE cust.id = c.id), 0) AS currentBalance "
            + "FROM t_report_daily_electricity r "
            + "JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0 "
            + "LEFT JOIN t_customer c ON m.customer_id = c.id "
            + "LEFT JOIN t_meter_group g ON m.group_id = g.id "
            + "WHERE 1=1 "
            + "<if test='customerId != null'> AND r.customer_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND r.group_id = #{groupId} </if>"
            + "GROUP BY r.meter_id, m.steal_no, m.db_no, c.id, c.customer_name, c.balance, g.group_name "
            + "ORDER BY m.bjlx, r.meter_id"
            + "</script>")
    List<Map<String, Object>> billingYearlyDetailByMeter(@Param("year") String year,
                                                          @Param("statMonth") String statMonth,
                                                          @Param("customerId") Long customerId,
                                                          @Param("groupId") Long groupId);

    /**
     * 查询指定月份内，已删除电表各日的用电量明细。
     * <p>
     * 从预聚合表 t_report_daily_electricity 逐日提取，只包含已逻辑删除的 meter_id。
     * 用于填充 DeletedMeterReportVO.dailyUsage 的每日列。
     * </p>
     *
     * @param statMonth 统计月份（yyyy-MM）
     * @return [{meterId, dayKey（dd, 如 "01"）, dailyConsumed}]
     */
    @Select("SELECT rde.meter_id AS meterId, "
            + "       DATE_FORMAT(rde.report_date, '%m-%d') AS dayKey, "
            + "       COALESCE(SUM(rde.electricity_consumed), 0) AS dailyConsumed "
            + "FROM t_report_daily_electricity rde "
            + "JOIN t_meter m ON rde.meter_id = m.id AND m.is_deleted = 1 "
            + "WHERE DATE_FORMAT(rde.report_date, '%Y-%m') = #{statMonth} "
            + "GROUP BY rde.meter_id, DATE_FORMAT(rde.report_date, '%m-%d') "
            + "ORDER BY rde.meter_id, dayKey")
    List<Map<String, Object>> dailyConsumptionForDeletedMeters(@Param("statMonth") String statMonth);

    // ──────────────── 月度用电量汇总明细（按表计×月度，含正向/反向/余额） ────────────────

    /**
     * 查询月度用电量汇总明细（按表计×月度维度）。
     * <p>
     * 从 tb_meter_monthly_report 读取预聚合数据，
     * JOIN t_meter / t_customer / t_meter_group 获取维度名称，
     * LEFT JOIN t_shadow_meter 获取虚拟表信息。
     * 支持按统计月份、商户ID、分组ID筛选。
     *
     * @param statMonth 统计月份（yyyy-MM）
     * @param customerId 商户ID（NULL=全量）
     * @param groupId    分组ID（NULL=全量）
     * @return 月度用电量汇总明细列表
     */
    @Select("<script>"
            + "SELECT r.meter_id AS meterId, "
            + "       COALESCE(NULLIF(m.steal_no, ''), m.db_no) AS deviceNo, "
            + "       COALESCE(c.customer_name, '') AS customerName, "
            + "       COALESCE(g.group_name, '') AS groupName, "
            + "       CASE m.bjlx "
            + "         WHEN 1 THEN '租户' "
            + "         WHEN 2 THEN '总表' "
            + "         WHEN 3 THEN '公共' "
            + "         WHEN 4 THEN '其他' "
            + "         ELSE '未知' "
            + "       END AS meterType, "
            + "       r.stat_month AS statMonth, "
            + "       r.forward_start_reading AS forwardStartReading, "
            + "       r.forward_start_time AS forwardStartTime, "
            + "       r.forward_end_reading AS forwardEndReading, "
            + "       r.forward_end_time AS forwardEndTime, "
            + "       r.forward_usage AS forwardUsage, "
            + "       r.reverse_start_reading AS reverseStartReading, "
            + "       r.reverse_start_time AS reverseStartTime, "
            + "       r.reverse_end_reading AS reverseEndReading, "
            + "       r.reverse_end_time AS reverseEndTime, "
            + "       r.reverse_usage AS reverseUsage, "
            + "       r.reverse_ratio AS reverseRatio, "
            + "       r.total_start_reading AS totalStartReading, "
            + "       r.total_end_reading AS totalEndReading, "
            + "       COALESCE(r.total_usage, 0) AS totalEnergy, "
            + "       r.previous_balance AS previousBalance, "
            + "       r.current_balance AS currentBalance, "
            + "       r.data_update_time AS dataUpdateTime, "
            + "       vm.steal_no AS virtualMeterNo, "
            + "       vc.customer_name AS virtualMerchantName "
            + "FROM tb_meter_monthly_report r "
            + "JOIN t_meter m ON r.meter_id = m.id AND m.is_deleted = 0 "
            + "LEFT JOIN t_customer c ON m.customer_id = c.id "
            + "LEFT JOIN t_meter_group g ON m.group_id = g.id "
            + "LEFT JOIN t_shadow_meter sm ON sm.parent_meter_id = m.id AND sm.is_deleted = 0 AND sm.status = 1 "
            + "LEFT JOIN t_meter vm ON sm.child_meter_id = vm.id "
            + "LEFT JOIN t_customer vc ON vm.customer_id = vc.id "
            + "WHERE r.meter_type = 'ELECTRIC' "
            + "<if test='statMonth != null and statMonth != \"\"'> AND r.stat_month = #{statMonth} </if>"
            + "<if test='customerId != null'> AND r.merchant_id = #{customerId} </if>"
            + "<if test='groupId != null'> AND r.group_id = #{groupId} </if>"
            + "ORDER BY r.stat_month DESC, m.bjlx, r.meter_id"
            + "</script>")
    List<Map<String, Object>> monthlySummaryByMeter(@Param("statMonth") String statMonth,
                                                     @Param("customerId") Long customerId,
                                                     @Param("groupId") Long groupId);
}
