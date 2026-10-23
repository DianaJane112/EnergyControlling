package com.competition.energycontrollerplatform.miniapp.service;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.VirtualMeterViewerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MiniAppMeterAccessServiceTest {
    @Mock MeterMapper meterMapper;
    @Mock VirtualMeterViewerMapper viewerMapper;
    private MiniAppMeterAccessService service;

    @BeforeEach
    void setUp() {
        service = new MiniAppMeterAccessService(meterMapper, viewerMapper);
    }

    @Test
    void accountOwnerCanViewOwnMeter() {
        Meter meter = meter(10L, 20L, "VIRTUAL");
        when(meterMapper.selectById(10L)).thenReturn(meter);
        assertSame(meter, service.requireViewable(1L, 20L, 10L));
    }

    @Test
    void authorizedViewerCanViewVirtualMeterWithoutCustomerBinding() {
        Meter meter = meter(10L, 20L, "VIRTUAL");
        when(meterMapper.selectById(10L)).thenReturn(meter);
        when(viewerMapper.countActivePermission(10L, 7L)).thenReturn(1);
        assertSame(meter, service.requireViewable(7L, null, 10L));
    }

    @Test
    void unauthorizedViewerIsRejected() {
        when(meterMapper.selectById(10L)).thenReturn(meter(10L, 20L, "VIRTUAL"));
        assertThrows(BusinessException.class, () -> service.requireViewable(7L, 99L, 10L));
    }

    private Meter meter(Long id, Long customerId, String kind) {
        Meter meter = new Meter();
        meter.setId(id);
        meter.setCustomerId(customerId);
        meter.setMeterKind(kind);
        meter.setIsDeleted(0);
        return meter;
    }
}
