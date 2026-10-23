package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.PriceStrategy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.time.LocalDate;

/**
 * 费率时段策略 Mapper。
 */
@Mapper
public interface PriceStrategyMapper extends BaseMapper<PriceStrategy> {

    /**
     * 查询指定项目下当前生效的策略列表。
     * <p>规则：status=1 且 (expire_date IS NULL OR expire_date >= CURDATE()) 且 effective_date <= CURDATE()</p>
     */
    @Select("SELECT * FROM t_price_strategy WHERE status = 1 AND is_deleted = 0 "
            + "AND effective_date <= CURDATE() "
            + "AND (expire_date IS NULL OR expire_date >= CURDATE()) "
            + "AND (project_id = #{projectId} OR project_id IS NULL) "
            + "ORDER BY project_id DESC, create_time DESC")
    List<PriceStrategy> findActiveByProject(@Param("projectId") Long projectId);

    /**
     * 查询全局默认策略。
     */
    @Select("SELECT * FROM t_price_strategy WHERE project_id IS NULL AND status = 1 AND is_deleted = 0 "
            + "AND effective_date <= CURDATE() AND (expire_date IS NULL OR expire_date >= CURDATE()) "
            + "ORDER BY create_time DESC LIMIT 1")
    PriceStrategy findDefaultActive();

    /**
     * 逻辑删除费率策略（绕过 @TableLogic 的 update_time 空值问题）。
     */
    @Update("UPDATE t_price_strategy SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);

    @Select("SELECT * FROM t_price_strategy WHERE version_group_code=#{groupCode} AND meter_type=#{meterType} " +
            "AND status=1 AND is_deleted=0 AND effective_date<=#{billingDate} " +
            "AND (expire_date IS NULL OR expire_date>=#{billingDate}) ORDER BY version_no DESC, effective_date DESC LIMIT 1")
    PriceStrategy findEffectiveVersion(@Param("groupCode") String groupCode,
                                       @Param("meterType") Integer meterType,
                                       @Param("billingDate") LocalDate billingDate);

    @Select("SELECT COUNT(*) FROM t_price_strategy WHERE version_group_code=#{groupCode} AND meter_type=#{meterType} " +
            "AND is_deleted=0 AND effective_date<=COALESCE(#{expireDate}, '9999-12-31') " +
            "AND COALESCE(expire_date, '9999-12-31')>=#{effectiveDate}")
    int countOverlapping(@Param("groupCode") String groupCode, @Param("meterType") Integer meterType,
                         @Param("effectiveDate") LocalDate effectiveDate, @Param("expireDate") LocalDate expireDate);

    @Select("SELECT COUNT(*) FROM t_price_strategy WHERE id&lt;&gt;#{id} AND version_group_code=#{groupCode} " +
            "AND meter_type=#{meterType} AND is_deleted=0 " +
            "AND effective_date&lt;=COALESCE(#{expireDate}, '9999-12-31') " +
            "AND COALESCE(expire_date, '9999-12-31')&gt;=#{effectiveDate}")
    int countOverlappingExcluding(@Param("id") Long id, @Param("groupCode") String groupCode,
            @Param("meterType") Integer meterType, @Param("effectiveDate") LocalDate effectiveDate,
            @Param("expireDate") LocalDate expireDate);

    @Select("SELECT COALESCE(MAX(version_no),0) FROM t_price_strategy WHERE version_group_code=#{groupCode} AND meter_type=#{meterType}")
    int maxVersionNo(@Param("groupCode") String groupCode, @Param("meterType") Integer meterType);
}
