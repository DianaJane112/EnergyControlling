package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.RechargeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrder> {

    /**
     * 按日聚合充值统计。
     * <p>直接在 MySQL 层面使用 SUM + GROUP BY，严禁将数据捞回 JVM 做 for 循环累加。</p>
     *
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate   结束日期 (yyyy-MM-dd)
     * @param customerId 商户ID（NULL=全量）
     * @return [{period, totalAmount, chargeCount, customerCount}]
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS period, "
            + "       COALESCE(SUM(amount), 0) AS totalAmount, "
            + "       COUNT(*) AS chargeCount, "
            + "       COUNT(DISTINCT customer_id) AS customerCount "
            + "FROM t_recharge_order "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND create_time &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND create_time &lt;= CONCAT(#{endDate}, ' 23:59:59') </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByDay(@Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("customerId") Long customerId);

    /**
     * 按月聚合充值统计。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(create_time, '%Y-%m') AS period, "
            + "       COALESCE(SUM(amount), 0) AS totalAmount, "
            + "       COUNT(*) AS chargeCount, "
            + "       COUNT(DISTINCT customer_id) AS customerCount "
            + "FROM t_recharge_order "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND create_time &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND create_time &lt;= CONCAT(#{endDate}, ' 23:59:59') </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "GROUP BY DATE_FORMAT(create_time, '%Y-%m') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByMonth(@Param("startDate") String startDate,
                                          @Param("endDate") String endDate,
                                          @Param("customerId") Long customerId);

    /**
     * 按年聚合充值统计。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(create_time, '%Y') AS period, "
            + "       COALESCE(SUM(amount), 0) AS totalAmount, "
            + "       COUNT(*) AS chargeCount, "
            + "       COUNT(DISTINCT customer_id) AS customerCount "
            + "FROM t_recharge_order "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND create_time &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND create_time &lt;= CONCAT(#{endDate}, ' 23:59:59') </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "GROUP BY DATE_FORMAT(create_time, '%Y') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByYear(@Param("startDate") String startDate,
                                         @Param("endDate") String endDate,
                                         @Param("customerId") Long customerId);
}
