package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.BillingSegment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Mapper
public interface BillingSegmentMapper extends BaseMapper<BillingSegment> {
    @Select("SELECT COALESCE(SUM(billed_consumption), 0) FROM t_billing_segment " +
            "WHERE is_deleted=0 AND virtual_meter_id=#{virtualMeterId} " +
            "AND period_end&gt;=#{periodStart} AND period_end&lt;#{beforeTime}")
    java.math.BigDecimal sumPeriodConsumption(@Param("virtualMeterId") Long virtualMeterId,
        @Param("periodStart") LocalDateTime periodStart, @Param("beforeTime") LocalDateTime beforeTime);

    @Select("<script>SELECT " +
            "CASE WHEN #{periodType}='DAY' THEN DATE_FORMAT(billing_date,'%Y-%m-%d') " +
            "WHEN #{periodType}='YEAR' THEN DATE_FORMAT(billing_date,'%Y') ELSE DATE_FORMAT(billing_date,'%Y-%m') END period, " +
            "CASE WHEN #{dimension}='PROJECT' THEN project_id WHEN #{dimension}='AREA' THEN area_group_id " +
            "WHEN #{dimension}='CATEGORY' THEN category_group_id ELSE virtual_meter_id END dimensionId, " +
            "CASE WHEN #{dimension}='PROJECT' THEN project_name WHEN #{dimension}='AREA' THEN area_name " +
            "WHEN #{dimension}='CATEGORY' THEN category_name ELSE CONCAT('虚拟账户#',virtual_meter_id) END dimensionName, " +
            "COUNT(DISTINCT source_meter_id) meterCount, COUNT(DISTINCT virtual_meter_id) virtualMeterCount, " +
            "COALESCE(SUM(billed_consumption),0) totalConsumption, COALESCE(SUM(charge_amount),0) totalCharge, " +
            "CASE WHEN SUM(billed_consumption)=0 THEN 0 ELSE SUM(charge_amount)/SUM(billed_consumption) END averagePrice " +
            "FROM t_billing_segment WHERE is_deleted=0 " +
            "<if test='startDate!=null'>AND billing_date&gt;=#{startDate}</if> " +
            "<if test='endDate!=null'>AND billing_date&lt;=#{endDate}</if> " +
            "<if test='customerId!=null'>AND customer_id=#{customerId}</if> " +
            "<if test='projectId!=null'>AND project_id=#{projectId}</if> " +
            "<if test='areaGroupId!=null'>AND area_group_id=#{areaGroupId}</if> " +
            "<if test='categoryGroupId!=null'>AND category_group_id=#{categoryGroupId}</if> " +
            "<if test='virtualMeterId!=null'>AND virtual_meter_id=#{virtualMeterId}</if> " +
            "GROUP BY period, dimensionId, dimensionName ORDER BY period DESC, dimensionName</script>")
    List<Map<String,Object>> dimensionSummary(@Param("periodType") String periodType,
        @Param("dimension") String dimension, @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate, @Param("customerId") Long customerId,
        @Param("projectId") Long projectId, @Param("areaGroupId") Long areaGroupId,
        @Param("categoryGroupId") Long categoryGroupId, @Param("virtualMeterId") Long virtualMeterId);
}
