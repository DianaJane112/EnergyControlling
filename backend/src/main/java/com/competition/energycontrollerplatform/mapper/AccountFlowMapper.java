package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.AccountFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface AccountFlowMapper extends BaseMapper<AccountFlow> {

    /** 今日累计充值总额（trade_type=1） */
    @Select("SELECT COALESCE(SUM(change_amount), 0) FROM t_account_flow " +
            "WHERE trade_type = 1 AND DATE(operate_time) = CURDATE() AND is_deleted = 0")
    BigDecimal sumTodayRecharge();

    /** 今日累计能耗扣费总额（trade_type=2，取绝对值） */
    @Select("SELECT COALESCE(SUM(ABS(change_amount)), 0) FROM t_account_flow " +
            "WHERE trade_type = 2 AND DATE(operate_time) = CURDATE() AND is_deleted = 0")
    BigDecimal sumTodayDebit();

    // ──────────────── 按表计类型分组的计费聚合（§五.8/9/10 水表计费报表） ────────────────

    /**
     * 按日聚合计费金额（按 meterType 筛选）。
     * <p>JOIN t_meter 过滤表计类型，LEFT JOIN tb_meter_daily_report 获取总用量。</p>
     */
    @Select("<script>"
            + "SELECT DATE(a.operate_time) AS period, "
            + "       COALESCE(SUM(ABS(a.change_amount)), 0) AS totalChargeAmount, "
            + "       COALESCE(MAX(u.totalConsumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT a.meter_id) AS meterCount "
            + "FROM t_account_flow a "
            + "INNER JOIN t_meter m ON a.meter_id = m.id "
            + "<if test='meterType != null and meterType != \"\"'> AND m.meter_type = "
            + "  CASE WHEN #{meterType} = 'ELECTRIC' THEN 1 ELSE 0 END </if>"
            + "LEFT JOIN ("
            + "  SELECT stat_date, SUM(total_usage) AS totalConsumed "
            + "  FROM tb_meter_daily_report WHERE 1=1 "
            + "  <if test='meterType != null and meterType != \"\"'> AND meter_type = #{meterType} </if>"
            + "  GROUP BY stat_date"
            + ") u ON DATE(a.operate_time) = u.stat_date "
            + "WHERE a.trade_type = 2 AND a.is_deleted = 0 "
            + "  AND a.meter_id IS NOT NULL "
            + "<if test='startDate != null'> AND DATE(a.operate_time) &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND DATE(a.operate_time) &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND a.customer_id = #{customerId} </if>"
            + "GROUP BY DATE(a.operate_time) "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumBillingByDate(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("customerId") Long customerId,
                                                @Param("meterType") String meterType);

    /**
     * 按月聚合计费金额（按 meterType 筛选）。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(a.operate_time, '%Y-%m') AS period, "
            + "       COALESCE(SUM(ABS(a.change_amount)), 0) AS totalChargeAmount, "
            + "       COALESCE(MAX(u.totalConsumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT a.meter_id) AS meterCount "
            + "FROM t_account_flow a "
            + "INNER JOIN t_meter m ON a.meter_id = m.id "
            + "<if test='meterType != null and meterType != \"\"'> AND m.meter_type = "
            + "  CASE WHEN #{meterType} = 'ELECTRIC' THEN 1 ELSE 0 END </if>"
            + "LEFT JOIN ("
            + "  SELECT DATE_FORMAT(stat_date, '%Y-%m') AS statMonth, SUM(total_usage) AS totalConsumed "
            + "  FROM tb_meter_daily_report WHERE 1=1 "
            + "  <if test='meterType != null and meterType != \"\"'> AND meter_type = #{meterType} </if>"
            + "  GROUP BY DATE_FORMAT(stat_date, '%Y-%m')"
            + ") u ON DATE_FORMAT(a.operate_time, '%Y-%m') = u.statMonth "
            + "WHERE a.trade_type = 2 AND a.is_deleted = 0 "
            + "  AND a.meter_id IS NOT NULL "
            + "<if test='startDate != null'> AND DATE(a.operate_time) &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND DATE(a.operate_time) &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND a.customer_id = #{customerId} </if>"
            + "GROUP BY DATE_FORMAT(a.operate_time, '%Y-%m') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumBillingByMonth(@Param("startDate") String startDate,
                                                 @Param("endDate") String endDate,
                                                 @Param("customerId") Long customerId,
                                                 @Param("meterType") String meterType);

    /**
     * 按年聚合计费金额（按 meterType 筛选）。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(a.operate_time, '%Y') AS period, "
            + "       COALESCE(SUM(ABS(a.change_amount)), 0) AS totalChargeAmount, "
            + "       COALESCE(MAX(u.totalConsumed), 0) AS totalConsumed, "
            + "       COUNT(DISTINCT a.meter_id) AS meterCount "
            + "FROM t_account_flow a "
            + "INNER JOIN t_meter m ON a.meter_id = m.id "
            + "<if test='meterType != null and meterType != \"\"'> AND m.meter_type = "
            + "  CASE WHEN #{meterType} = 'ELECTRIC' THEN 1 ELSE 0 END </if>"
            + "LEFT JOIN ("
            + "  SELECT DATE_FORMAT(stat_date, '%Y') AS statYear, SUM(total_usage) AS totalConsumed "
            + "  FROM tb_meter_daily_report WHERE 1=1 "
            + "  <if test='meterType != null and meterType != \"\"'> AND meter_type = #{meterType} </if>"
            + "  GROUP BY DATE_FORMAT(stat_date, '%Y')"
            + ") u ON DATE_FORMAT(a.operate_time, '%Y') = u.statYear "
            + "WHERE a.trade_type = 2 AND a.is_deleted = 0 "
            + "  AND a.meter_id IS NOT NULL "
            + "<if test='startDate != null'> AND DATE(a.operate_time) &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND DATE(a.operate_time) &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND a.customer_id = #{customerId} </if>"
            + "GROUP BY DATE_FORMAT(a.operate_time, '%Y') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumBillingByYear(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("customerId") Long customerId,
                                                @Param("meterType") String meterType);

    // ──────────────── 分账统计聚合（按日/月/年） ────────────────

    /**
     * 按日聚合分账统计（充值/扣费/退费/调账/分摊/净流水/笔数）。
     */
    @Select("<script>"
            + "SELECT DATE(operate_time) AS period, "
            + "       COALESCE(SUM(CASE WHEN trade_type IN (1,5) THEN change_amount ELSE 0 END), 0) AS totalRecharge, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 2 THEN ABS(change_amount) ELSE 0 END), 0) AS totalDebit, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 3 THEN ABS(change_amount) ELSE 0 END), 0) AS totalRefund, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 4 THEN change_amount ELSE 0 END), 0) AS totalAdjust, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 7 THEN ABS(change_amount) ELSE 0 END), 0) AS totalAllocation, "
            + "       COALESCE(SUM(change_amount), 0) AS netFlow, "
            + "       COUNT(*) AS flowCount "
            + "FROM t_account_flow "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND operate_time &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND operate_time &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "GROUP BY DATE(operate_time) "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumFlowByDate(@Param("startDate") String startDate,
                                            @Param("endDate") String endDate,
                                            @Param("customerId") Long customerId);

    /**
     * 按月聚合分账统计。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(operate_time, '%Y-%m') AS period, "
            + "       COALESCE(SUM(CASE WHEN trade_type IN (1,5) THEN change_amount ELSE 0 END), 0) AS totalRecharge, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 2 THEN ABS(change_amount) ELSE 0 END), 0) AS totalDebit, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 3 THEN ABS(change_amount) ELSE 0 END), 0) AS totalRefund, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 4 THEN change_amount ELSE 0 END), 0) AS totalAdjust, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 7 THEN ABS(change_amount) ELSE 0 END), 0) AS totalAllocation, "
            + "       COALESCE(SUM(change_amount), 0) AS netFlow, "
            + "       COUNT(*) AS flowCount "
            + "FROM t_account_flow "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND operate_time &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND operate_time &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "GROUP BY DATE_FORMAT(operate_time, '%Y-%m') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumFlowByMonth(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("customerId") Long customerId);

    /**
     * 按年聚合分账统计。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(operate_time, '%Y') AS period, "
            + "       COALESCE(SUM(CASE WHEN trade_type IN (1,5) THEN change_amount ELSE 0 END), 0) AS totalRecharge, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 2 THEN ABS(change_amount) ELSE 0 END), 0) AS totalDebit, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 3 THEN ABS(change_amount) ELSE 0 END), 0) AS totalRefund, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 4 THEN change_amount ELSE 0 END), 0) AS totalAdjust, "
            + "       COALESCE(SUM(CASE WHEN trade_type = 7 THEN ABS(change_amount) ELSE 0 END), 0) AS totalAllocation, "
            + "       COALESCE(SUM(change_amount), 0) AS netFlow, "
            + "       COUNT(*) AS flowCount "
            + "FROM t_account_flow "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND operate_time &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND operate_time &lt;= #{endDate} </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "GROUP BY DATE_FORMAT(operate_time, '%Y') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumFlowByYear(@Param("startDate") String startDate,
                                            @Param("endDate") String endDate,
                                            @Param("customerId") Long customerId);

    /**
     * 逻辑删除资金流水记录（绕过 @TableLogic 的 update_time 空值问题）。
     */
    @Update("UPDATE t_account_flow SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
