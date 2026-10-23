package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.common.BusinessException;
import com.competition.energycontrollerplatform.common.CacheNullValueHelper;
import com.competition.energycontrollerplatform.dto.PriceStrategyRequest;
import com.competition.energycontrollerplatform.entity.PriceStrategy;
import com.competition.energycontrollerplatform.mapper.PriceStrategyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceStrategyServiceImplTest {
    @Mock private PriceStrategyMapper mapper;
    @Mock private CacheNullValueHelper cacheNullValueHelper;
    private PriceStrategyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PriceStrategyServiceImpl(mapper, cacheNullValueHelper, new ObjectMapper());
    }

    @Test
    void createRejectsOverlappingVersionPeriod() {
        PriceStrategyRequest request = request(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
        when(mapper.countOverlapping("ELEC-GROUP", 1, request.getEffectiveDate(), request.getExpireDate()))
                .thenReturn(1);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void createAssignsNextVersionForNonOverlappingPeriod() {
        PriceStrategyRequest request = request(LocalDate.of(2027, 1, 1), null);
        when(mapper.countOverlapping("ELEC-GROUP", 1, request.getEffectiveDate(), null)).thenReturn(0);
        when(mapper.maxVersionNo("ELEC-GROUP", 1)).thenReturn(2);

        service.create(request);

        ArgumentCaptor<PriceStrategy> captor = ArgumentCaptor.forClass(PriceStrategy.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getVersionGroupCode()).isEqualTo("ELEC-GROUP");
        assertThat(captor.getValue().getVersionNo()).isEqualTo(3);
    }

    @Test
    void createRejectsExpireDateBeforeEffectiveDate() {
        PriceStrategyRequest request = request(LocalDate.of(2027, 1, 2), LocalDate.of(2027, 1, 1));
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateRejectsAlreadyEffectiveStrategy() {
        PriceStrategy existing = new PriceStrategy();
        existing.setId(1L); existing.setMeterType(1);
        existing.setEffectiveDate(LocalDate.now().minusDays(1));
        when(mapper.selectById(1L)).thenReturn(existing);
        assertThatThrownBy(() -> service.update(1L, request(LocalDate.now().plusDays(1), null)))
                .isInstanceOf(BusinessException.class);
    }

    private PriceStrategyRequest request(LocalDate effective, LocalDate expire) {
        PriceStrategyRequest request = new PriceStrategyRequest();
        request.setStrategyName("electric tariff");
        request.setStrategyCode("ELEC-" + effective);
        request.setVersionGroupCode("ELEC-GROUP");
        request.setMeterType(1);
        request.setPriceType(1);
        request.setFixedPrice(new BigDecimal("0.85"));
        request.setPlusPrice(BigDecimal.ZERO);
        request.setEffectiveDate(effective);
        request.setExpireDate(expire);
        request.setStatus(1);
        return request;
    }
}
