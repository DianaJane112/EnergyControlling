package com.competition.energycontrollerplatform.miniapp.service;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.VirtualMeterViewerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 小程序表计数据访问权限的统一入口。 */
@Service
@RequiredArgsConstructor
public class MiniAppMeterAccessService {
    private final MeterMapper meterMapper;
    private final VirtualMeterViewerMapper viewerMapper;

    public Meter requireViewable(Long userId, Long customerId, Long meterId) {
        if (meterId == null) {
            return null;
        }
        Meter meter = meterMapper.selectById(meterId);
        if (meter == null || Integer.valueOf(1).equals(meter.getIsDeleted())) {
            throw new BusinessException(404, "表计不存在");
        }
        boolean owner = customerId != null && customerId.equals(meter.getCustomerId());
        boolean viewer = userId != null && "VIRTUAL".equalsIgnoreCase(meter.getMeterKind())
                && viewerMapper.countActivePermission(meterId, userId) > 0;
        if (!owner && !viewer) {
            throw new BusinessException(403, "无权查看该表计数据");
        }
        return meter;
    }
}
