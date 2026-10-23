package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.BalanceAdjustment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BalanceAdjustmentMapper extends BaseMapper<BalanceAdjustment> {

    /**
     * 按日聚合余额调整统计。
     * <p>直接在 MySQL 层面使用 SUM + GROUP BY。</p>
     *
     * @param startDate  开始日期 (yyyy-MM-dd)
     * @param endDate    结束日期 (yyyy-MM-dd)
     * @param customerId 商户ID（NULL=全量）
     * @return [{period, totalAmount, adjustCount, customerCount}]
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(adjustment_time, '%Y-%m-%d') AS period, "
            + "       COALESCE(SUM(balance), 0) AS totalAmount, "
            + "       COUNT(*) AS adjustCount, "
            + "       COUNT(DISTINCT customer_id) AS customerCount "
            + "FROM t_balance_adjustment "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND adjustment_time &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND adjustment_time &lt;= CONCAT(#{endDate}, ' 23:59:59') </if>"
            + "<if test='customerId != null'> AND customer_id = #{customerId} </if>"
            + "GROUP BY DATE_FORMAT(adjustment_time, '%Y-%m-%d') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByDay(@Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("customerId") Long customerId);
}
