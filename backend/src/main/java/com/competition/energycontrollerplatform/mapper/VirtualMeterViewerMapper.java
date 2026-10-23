package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.VirtualMeterViewer;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface VirtualMeterViewerMapper extends BaseMapper<VirtualMeterViewer> {
    @Select("SELECT * FROM t_virtual_meter_viewer WHERE virtual_meter_id=#{meterId} AND miniapp_user_id=#{userId} LIMIT 1")
    VirtualMeterViewer selectIncludingDeleted(@Param("meterId") Long meterId, @Param("userId") Long userId);

    @Update("UPDATE t_virtual_meter_viewer SET is_deleted=0,status=1,permission_type='VIEW',update_time=NOW() WHERE id=#{id}")
    int restore(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM t_virtual_meter_viewer WHERE virtual_meter_id=#{meterId} AND miniapp_user_id=#{userId} " +
            "AND status=1 AND is_deleted=0 AND (effective_time IS NULL OR effective_time<=NOW()) " +
            "AND (expire_time IS NULL OR expire_time>=NOW())")
    int countActivePermission(@Param("meterId") Long meterId, @Param("userId") Long userId);

    @Select("SELECT virtual_meter_id FROM t_virtual_meter_viewer WHERE miniapp_user_id=#{userId} " +
            "AND status=1 AND is_deleted=0 AND (effective_time IS NULL OR effective_time<=NOW()) " +
            "AND (expire_time IS NULL OR expire_time>=NOW())")
    List<Long> selectActiveMeterIds(@Param("userId") Long userId);
}
