package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.Collector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CollectorMapper extends BaseMapper<Collector> {

    /**
     * 逻辑删除集中器（绕过 MyBatis-Plus @TableLogic 的 update_time 空值问题）。
     * <p>MyBatis-Plus 的 deleteById 在逻辑删除时不会触发 MetaObjectHandler，
     * 导致 update_time 被设为 NULL，而数据库该列有 NOT NULL 约束。
     * 此方法直接执行原生 UPDATE，确保 update_time 被正确刷新。</p>
     *
     * @param id 集中器ID
     * @return 影响行数
     */
    @Update("UPDATE t_collector SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
