package com.competition.energycontrollerplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.dto.MeterGroupRequest;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.MeterGroup;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterGroupMapper;
import com.competition.energycontrollerplatform.security.SecurityUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterGroupServiceImplTest {
    @Mock MeterGroupMapper meterGroupMapper;
    @Mock CustomerMapper customerMapper;
    @InjectMocks MeterGroupServiceImpl service;

    @Test
    void rejectsDuplicateNameUnderSameParentAndMerchant() {
        MeterGroupRequest request = new MeterGroupRequest();
        request.setGroupName("D区1期");
        request.setMerchantId(7L);
        Customer customer = new Customer();
        customer.setId(7L);
        when(customerMapper.selectById(7L)).thenReturn(customer);
        when(meterGroupMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        try (MockedStatic<SecurityUserContext> security = mockStatic(SecurityUserContext.class)) {
            security.when(SecurityUserContext::isAdmin).thenReturn(true);
            assertThrows(BusinessException.class, () -> service.create(request));
        }
    }

    @Test
    void rejectsMovingNodeBelowItsDescendant() {
        MeterGroup group = group(1L, null, 7L);
        MeterGroup child = group(2L, 1L, 7L);
        MeterGroupRequest request = new MeterGroupRequest();
        request.setGroupName("根分组");
        request.setParentId(2L);
        when(meterGroupMapper.selectById(1L)).thenReturn(group);
        when(meterGroupMapper.selectById(2L)).thenReturn(child);

        try (MockedStatic<SecurityUserContext> security = mockStatic(SecurityUserContext.class)) {
            security.when(SecurityUserContext::isAdmin).thenReturn(true);
            assertThrows(BusinessException.class, () -> service.update(1L, request));
        }
    }

    private MeterGroup group(Long id, Long parentId, Long merchantId) {
        MeterGroup group = new MeterGroup();
        group.setId(id);
        group.setParentId(parentId);
        group.setMerchantId(merchantId);
        group.setGroupName("group-" + id);
        group.setIsDeleted(0);
        return group;
    }
}
