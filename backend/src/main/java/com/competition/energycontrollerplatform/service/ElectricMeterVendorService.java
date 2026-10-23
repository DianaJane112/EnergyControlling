package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.ElectricMeterVendorRequest;
import com.competition.energycontrollerplatform.entity.ElectricMeterVendor;

/**
 * 电表厂商对接配置服务。
 */
public interface ElectricMeterVendorService {

    IPage<ElectricMeterVendor> listPage(int pageNum, int pageSize, String keyword);

    ElectricMeterVendor getById(Long id);

    ElectricMeterVendor create(ElectricMeterVendorRequest request);

    void updateStatus(Long id, Integer status);
}
