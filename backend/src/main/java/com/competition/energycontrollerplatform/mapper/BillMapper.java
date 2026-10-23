package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.Bill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 账单记录 Mapper。
 */
@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    /** 逻辑删除单条（绕过 @TableLogic 的 update_time 空值问题） */
    @Update("UPDATE t_bill SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);

    /**
     * 逻辑删除批量（绕过 @TableLogic 的 update_time 空值问题）。
     * 使用 MyBatis 动态 SQL 的 {@code <script>} 标签支持 IN 子句。
     */
    @Update("<script>" +
            "UPDATE t_bill SET is_deleted = 1, update_time = NOW() " +
            "WHERE is_deleted = 0 AND id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int logicDeleteByIds(@Param("ids") List<Long> ids);
}
