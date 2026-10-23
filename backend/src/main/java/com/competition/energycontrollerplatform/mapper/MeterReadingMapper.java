package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.MeterReading;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 表计读数 Mapper
 */
@Mapper
public interface MeterReadingMapper extends BaseMapper<MeterReading> {

    /** 获取指定表计最新一条读数 */
    @Select("SELECT * FROM t_meter_reading WHERE meter_id = #{meterId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT 1")
    MeterReading getLatestByMeterId(@Param("meterId") Long meterId);

    /**
     * 获取指定表计在指定时间点之前的最新一条读数（用于日冻结/结算底数查询）。
     * @param meterId  表计ID
     * @param beforeTime 截止时间点
     */
    @Select("SELECT * FROM t_meter_reading WHERE meter_id = #{meterId} AND is_deleted = 0 "
            + "AND create_time <= #{beforeTime} ORDER BY create_time DESC LIMIT 1")
    MeterReading getLatestBeforeTime(@Param("meterId") Long meterId,
                                      @Param("beforeTime") java.time.LocalDateTime beforeTime);

    @Select("SELECT * FROM t_meter_reading WHERE meter_id=#{meterId} AND is_deleted=0 " +
            "AND `timestamp`>#{afterTime} AND `timestamp`<=#{endTime} ORDER BY `timestamp` ASC, id ASC")
    List<MeterReading> listForSettlement(@Param("meterId") Long meterId,
                                         @Param("afterTime") LocalDateTime afterTime,
                                         @Param("endTime") LocalDateTime endTime);

    /** 批量计算当天每块电表的累计有功电量差值，避免列表页产生 N+1 查询。 */
    @Select("<script>"
            + "SELECT meter_id AS meterId, "
            + "CASE WHEN COUNT(kwh) &gt;= 2 THEN GREATEST(MAX(kwh) - MIN(kwh), 0) ELSE NULL END AS todayActiveEnergy "
            + "FROM t_meter_reading "
            + "WHERE is_deleted = 0 AND kwh IS NOT NULL "
            + "AND `timestamp` &gt;= CURDATE() AND `timestamp` &lt; DATE_ADD(CURDATE(), INTERVAL 1 DAY) "
            + "AND meter_id IN "
            + "<foreach collection='meterIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY meter_id"
            + "</script>")
    List<Map<String, Object>> selectTodayActiveEnergy(@Param("meterIds") List<Long> meterIds);

    /** 批量查询每块表最新上报的累计正向有功电量。 */
    @Select("<script>"
            + "SELECT r.meter_id AS meterId, r.kwh AS currentActiveEnergy "
            + "FROM t_meter_reading r INNER JOIN ("
            + "  SELECT meter_id, MAX(id) AS maxId FROM t_meter_reading "
            + "  WHERE is_deleted = 0 AND kwh IS NOT NULL AND meter_id IN "
            + "  <foreach collection='meterIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "  GROUP BY meter_id"
            + ") latest ON latest.maxId = r.id"
            + "</script>")
    List<Map<String, Object>> selectLatestActiveEnergy(@Param("meterIds") List<Long> meterIds);
}
