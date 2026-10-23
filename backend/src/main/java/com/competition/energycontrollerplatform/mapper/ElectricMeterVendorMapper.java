package com.competition.energycontrollerplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.energycontrollerplatform.entity.ElectricMeterVendor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ElectricMeterVendorMapper extends BaseMapper<ElectricMeterVendor> {

    /**
     * 停用除当前配置外的其他电表厂商配置。
     */
    @Update("UPDATE t_electric_meter_vendor SET status = 0, update_time = NOW() "
            + "WHERE id <> #{id} AND is_deleted = 0")
    int disableOthers(@Param("id") Long id);

    /**
     * 逻辑删除电表厂商配置。
     */
    @Update("UPDATE t_electric_meter_vendor SET is_deleted = 1, update_time = NOW() "
            + "WHERE id = #{id} AND is_deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
