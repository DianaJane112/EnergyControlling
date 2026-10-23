package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.BillCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 账单分类 Mapper。
 */
@Mapper
public interface BillCategoryMapper extends BaseMapper<BillCategory> {

    /**
     * 逻辑删除账单分类（绕过 MyBatis-Plus @TableLogic 的 update_time 空值问题）。
     */
    @Update("UPDATE t_bill_category SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
