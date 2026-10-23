package com.competition.energycontrollerplatform.service;

import com.competition.energycontrollerplatform.dto.*;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;
import java.util.List;

public interface VirtualMeterViewerService {
    List<VirtualMeterViewerVO> list(Long virtualMeterId);
    VirtualMeterViewerVO bind(Long virtualMeterId, VirtualMeterViewerRequest request);
    void delete(Long virtualMeterId, Long viewerId);
    void updateStatus(Long virtualMeterId, Long viewerId, Integer status);
    VirtualMeterAccountVO accountDetail(Long virtualMeterId);
    List<MiniAppUser> candidates(String keyword);
}
