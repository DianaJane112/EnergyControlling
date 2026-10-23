package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文件记录 Mapper。
 * <p>
 * 继承 MyBatis-Plus {@link BaseMapper}，自动获得 CRUD、分页等基础能力。
 * 复杂查询（如按模块+关联ID联表）通过 {@code LambdaQueryWrapper} 在 Service 层构建。
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {

    /**
     * 逻辑删除文件记录（绕过 @TableLogic 的 update_time 空值问题）。
     */
    @Update("UPDATE t_file_record SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
