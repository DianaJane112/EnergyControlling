package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.PublicAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PublicAllocationMapper extends BaseMapper<PublicAllocation> {

    /**
     * 按日聚合公共分摊统计。
     * <p>直接在 MySQL 层面使用 SUM + GROUP BY，严禁将数据捞回 JVM 做 for 循环累加。</p>
     *
     * @param startDate      开始日期 (yyyy-MM-dd)
     * @param endDate        结束日期 (yyyy-MM-dd)
     * @param allocationType 分摊方式（NULL=全量）
     * @return [{period, totalAmount, allocationCount}]
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(effective_date, '%Y-%m-%d') AS period, "
            + "       COALESCE(SUM(total_amount), 0) AS totalAmount, "
            + "       COUNT(*) AS allocationCount "
            + "FROM t_public_allocation "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND effective_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND effective_date &lt;= #{endDate} </if>"
            + "<if test='allocationType != null'> AND allocation_type = #{allocationType} </if>"
            + "GROUP BY DATE_FORMAT(effective_date, '%Y-%m-%d') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByDay(@Param("startDate") String startDate,
                                       @Param("endDate") String endDate,
                                       @Param("allocationType") String allocationType);

    /**
     * 按月聚合公共分摊统计。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(effective_date, '%Y-%m') AS period, "
            + "       COALESCE(SUM(total_amount), 0) AS totalAmount, "
            + "       COUNT(*) AS allocationCount "
            + "FROM t_public_allocation "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND effective_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND effective_date &lt;= #{endDate} </if>"
            + "<if test='allocationType != null'> AND allocation_type = #{allocationType} </if>"
            + "GROUP BY DATE_FORMAT(effective_date, '%Y-%m') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByMonth(@Param("startDate") String startDate,
                                         @Param("endDate") String endDate,
                                         @Param("allocationType") String allocationType);

    /**
     * 按年聚合公共分摊统计。
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(effective_date, '%Y') AS period, "
            + "       COALESCE(SUM(total_amount), 0) AS totalAmount, "
            + "       COUNT(*) AS allocationCount "
            + "FROM t_public_allocation "
            + "WHERE is_deleted = 0 "
            + "<if test='startDate != null'> AND effective_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND effective_date &lt;= #{endDate} </if>"
            + "<if test='allocationType != null'> AND allocation_type = #{allocationType} </if>"
            + "GROUP BY DATE_FORMAT(effective_date, '%Y') "
            + "ORDER BY period DESC"
            + "</script>")
    List<Map<String, Object>> sumByYear(@Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("allocationType") String allocationType);
}
