package com.competition.energycontrollerplatform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.energycontrollerplatform.dto.WaterMeterVendorRequest;
import com.competition.energycontrollerplatform.entity.WaterMeterVendor;

/**
 * 水表厂商对接配置服务。
 */
public interface WaterMeterVendorService {

    IPage<WaterMeterVendor> listPage(int pageNum, int pageSize, String keyword);

    WaterMeterVendor getById(Long id);

    WaterMeterVendor create(WaterMeterVendorRequest request);

    void updateStatus(Long id, Integer status);
}
