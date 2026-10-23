package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.VirtualMeterViewerRequest;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.VirtualMeterViewer;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.VirtualMeterViewerMapper;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;
import com.competition.energycontrollerplatform.miniapp.mapper.MiniAppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VirtualMeterViewerServiceImplTest {
    @Mock VirtualMeterViewerMapper viewerMapper;
    @Mock MeterMapper meterMapper;
    @Mock CustomerMapper customerMapper;
    @Mock MiniAppUserMapper userMapper;
    VirtualMeterViewerServiceImpl service;

    @BeforeEach void setUp() {
        service = new VirtualMeterViewerServiceImpl(viewerMapper, meterMapper, customerMapper, userMapper);
    }

    @Test void bindsViewerToVirtualMeter() {
        when(meterMapper.selectById(10L)).thenReturn(virtualMeter(10L, 20L));
        MiniAppUser user = new MiniAppUser(); user.setId(30L); user.setNickname("viewer");
        when(userMapper.selectById(30L)).thenReturn(user);
        when(viewerMapper.selectIncludingDeleted(10L, 30L)).thenReturn(null);
        doAnswer(inv -> { ((VirtualMeterViewer) inv.getArgument(0)).setId(40L); return 1; })
                .when(viewerMapper).insert(any(VirtualMeterViewer.class));
        VirtualMeterViewerRequest request = new VirtualMeterViewerRequest(); request.setMiniappUserId(30L);

        assertThat(service.bind(10L, request).getMiniappUserId()).isEqualTo(30L);
        ArgumentCaptor<VirtualMeterViewer> captor = ArgumentCaptor.forClass(VirtualMeterViewer.class);
        verify(viewerMapper).insert(captor.capture());
        assertThat(captor.getValue().getPermissionType()).isEqualTo("VIEW");
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test void rejectsViewerBindingToPhysicalMeter() {
        Meter meter = virtualMeter(10L, 20L); meter.setMeterKind("PHYSICAL");
        when(meterMapper.selectById(10L)).thenReturn(meter);
        VirtualMeterViewerRequest request = new VirtualMeterViewerRequest(); request.setMiniappUserId(30L);
        assertThatThrownBy(() -> service.bind(10L, request))
                .isInstanceOf(BusinessException.class).hasMessage("虚拟表不存在");
        verifyNoInteractions(userMapper);
    }

    @Test void returnsSettlementAccountDetail() {
        when(meterMapper.selectById(10L)).thenReturn(virtualMeter(10L, 20L));
        Customer customer = new Customer(); customer.setId(20L); customer.setCustomerCode("VA1");
        customer.setCustomerName("统一结算账户"); customer.setBalance(new BigDecimal("88.50"));
        when(customerMapper.selectById(20L)).thenReturn(customer);
        assertThat(service.accountDetail(10L).getBalance()).isEqualByComparingTo("88.50");
        assertThat(service.accountDetail(10L).getCustomerName()).isEqualTo("统一结算账户");
    }

    private Meter virtualMeter(Long id, Long customerId) {
        Meter meter = new Meter(); meter.setId(id); meter.setCustomerId(customerId);
        meter.setMeterKind("VIRTUAL"); meter.setDbNo("VME0001"); meter.setMeterType(1);
        return meter;
    }
}
