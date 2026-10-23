package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.MeterGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 分组管理 Mapper。
 */
@Mapper
public interface MeterGroupMapper extends BaseMapper<MeterGroup> {

    /**
     * 逻辑删除分组（绕过 @TableLogic 对 updateById 的拦截）。
     * <p>
     * MyBatis-Plus 的 @TableLogic 在 updateById 时不会将 is_deleted 写入 SET 子句，
     * 只将其作为 WHERE 条件，因此必须用原生 UPDATE 语句执行逻辑删除。
     * </p>
     *
     * @param id 分组ID
     * @return 影响行数（0 表示不存在或已被删除）
     */
    @Update("UPDATE t_meter_group SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
