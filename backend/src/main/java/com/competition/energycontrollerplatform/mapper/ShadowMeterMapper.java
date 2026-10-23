package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.ShadowMeter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 虚拟电表（总分表拓扑）Mapper。
 */
@Mapper
public interface ShadowMeterMapper extends BaseMapper<ShadowMeter> {

    @Select("SELECT sm.id relationId, sm.parent_meter_id virtualMeterId, sm.child_meter_id realMeterId, sm.status relationStatus, sm.create_time relationCreatedAt, m.meter_type meterType, COALESCE(m.db_no,m.steal_no) realMeterNo, m.name realMeterName, m.customer_id customerId, m.ct_ratio ctRatio, m.pt_ratio ptRatio, m.status meterStatus, m.online communicationStatus FROM t_shadow_meter sm JOIN t_meter m ON m.id=sm.child_meter_id WHERE sm.parent_meter_id=#{parentId} AND sm.is_deleted=0 ORDER BY sm.create_time DESC")
    List<Map<String,Object>> findRealMeters(@Param("parentId") Long parentId);

    /**
     * 批量统计每个虚拟电表（parent_meter_id）下关联的真实电表数量，避免列表页 N+1 查询。
     */
    @Select("<script>"
            + "SELECT parent_meter_id AS virtualMeterId, COUNT(*) AS cnt "
            + "FROM t_shadow_meter "
            + "WHERE shadow_type = 'REAL_METER' AND is_deleted = 0 "
            + "AND parent_meter_id IN "
            + "<foreach collection='parentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY parent_meter_id"
            + "</script>")
    List<Map<String,Object>> countRealMetersByParentIds(@Param("parentIds") List<Long> parentIds);

    /**
     * 查询指定虚拟电表下关联的真实电表关系（relationId + child_meter_id）。
     */
    @Select("SELECT id AS relationId, child_meter_id AS meterId FROM t_shadow_meter "
            + "WHERE parent_meter_id = #{virtualMeterId} AND shadow_type = 'REAL_METER' AND is_deleted = 0 "
            + "ORDER BY create_time DESC")
    List<Map<String,Object>> findRealMeterRelations(@Param("virtualMeterId") Long virtualMeterId);

    /**
     * 查询指定总表下所有启用的子表关系。
     */
    @Select("SELECT * FROM t_shadow_meter WHERE parent_meter_id = #{masterId} "
            + "AND status = 1 AND is_deleted = 0")
    List<ShadowMeter> findActiveByMasterId(@Param("masterId") Long masterId);

    /**
     * 查询指定子表所属的总表关系。
     */
    @Select("SELECT * FROM t_shadow_meter WHERE child_meter_id = #{childId} "
            + "AND status = 1 AND is_deleted = 0")
    List<ShadowMeter> findActiveByChildId(@Param("childId") Long childId);

    /**
     * 检查总分表关系是否已存在。
     */
    @Select("SELECT COUNT(*) FROM t_shadow_meter WHERE parent_meter_id = #{parentId} "
            + "AND child_meter_id = #{childId} AND is_deleted = 0")
    int countByParentAndChild(@Param("parentId") Long parentId, @Param("childId") Long childId);

    /**
     * 逻辑删除拓扑关系（绕过 MyBatis-Plus @TableLogic 的 update_time 空值问题）。
     */
    @Update("UPDATE t_shadow_meter SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);

    /**
     * 批量逻辑删除拓扑关系（绕过 MyBatis-Plus @TableLogic 的 update_time 空值问题）。
     */
    @Update("<script>"
            + "UPDATE t_shadow_meter SET is_deleted = 1, update_time = NOW() "
            + "WHERE id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + " AND is_deleted = 0"
            + "</script>")
    int logicDeleteBatchIds(@Param("ids") List<Long> ids);
}
