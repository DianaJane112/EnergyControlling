package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.entity.AccountFlow;
import com.competition.energycontrollerplatform.entity.BillingSegment;
import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.entity.MeterOperationTrace;
import com.competition.energycontrollerplatform.entity.MeterReading;
import com.competition.energycontrollerplatform.entity.PriceStrategy;
import com.competition.energycontrollerplatform.entity.ShadowMeter;
import com.competition.energycontrollerplatform.mapper.AccountFlowMapper;
import com.competition.energycontrollerplatform.mapper.BillingSegmentMapper;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.MeterGroupMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import com.competition.energycontrollerplatform.mapper.PriceStrategyMapper;
import com.competition.energycontrollerplatform.mapper.ProjectMapper;
import com.competition.energycontrollerplatform.mapper.ShadowMeterMapper;
import com.competition.energycontrollerplatform.service.MeterOperationTraceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyConsumptionServiceImplTest {

    @Mock private MeterMapper meterMapper;
    @Mock private MeterReadingMapper meterReadingMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private PriceStrategyMapper priceStrategyMapper;
    @Mock private ShadowMeterMapper shadowMeterMapper;
    @Mock private AccountFlowMapper accountFlowMapper;
    @Mock private MeterOperationTraceService meterOperationTraceService;
    @Mock private BillingSegmentMapper billingSegmentMapper;
    @Mock private MeterGroupMapper meterGroupMapper;
    @Mock private ProjectMapper projectMapper;

    private EnergyConsumptionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EnergyConsumptionServiceImpl(
                meterMapper, meterReadingMapper, customerMapper, priceStrategyMapper,
                shadowMeterMapper, accountFlowMapper, meterOperationTraceService, new ObjectMapper(),
                billingSegmentMapper, meterGroupMapper, projectMapper);
    }

    @Test
    void fixedPriceUsesStrategyPlusPriceAndTransformerRatios() {
        Meter meter = meter(10L, 100L, 20L);
        meter.setLastSettledReading(new BigDecimal("100"));
        meter.setCtRatio(new BigDecimal("2"));
        meter.setPtRatio(new BigDecimal("3"));
        MeterReading reading = reading(10L, "110");

        when(meterMapper.selectById(10L)).thenReturn(meter);
        when(shadowMeterMapper.findActiveByChildId(10L)).thenReturn(Collections.emptyList());
        when(meterReadingMapper.getLatestByMeterId(10L)).thenReturn(reading);
        when(priceStrategyMapper.selectById(20L)).thenReturn(fixedStrategy(20L, "0.80", "0.05"));
        when(customerMapper.subtractBalance(100L, new BigDecimal("51.00"))).thenReturn(1);
        when(customerMapper.selectById(100L)).thenReturn(customer(100L, "949.00"));

        assertThat(service.calculateAndDebit(10L, new BigDecimal("9.99")))
                .isEqualByComparingTo("51.00");

        ArgumentCaptor<MeterOperationTrace> traceCaptor = ArgumentCaptor.forClass(MeterOperationTrace.class);
        verify(meterOperationTraceService).record(traceCaptor.capture());
        MeterOperationTrace trace = traceCaptor.getValue();
        assertThat(trace.getConsumption()).isEqualByComparingTo("60");
        assertThat(trace.getUnitPrice()).isEqualByComparingTo("0.80");
        assertThat(trace.getPlusPrice()).isEqualByComparingTo("0.05");
        assertThat(trace.getRateConfigId()).isEqualTo(20L);
        assertThat(trace.getBillingType()).isEqualTo("FIXED");
    }

    @Test
    void realMeterBoundToVirtualMeterDebitsAndRecordsVirtualAccount() {
        Meter realMeter = meter(10L, 100L, 20L);
        realMeter.setLastSettledReading(new BigDecimal("5"));
        Meter virtualMeter = meter(99L, 200L, 30L);
        virtualMeter.setMeterKind("VIRTUAL");
        ShadowMeter relation = new ShadowMeter();
        relation.setParentMeterId(99L);
        relation.setChildMeterId(10L);
        relation.setShadowType("REAL_METER");

        when(meterMapper.selectById(10L)).thenReturn(realMeter);
        when(meterMapper.selectById(99L)).thenReturn(virtualMeter);
        when(shadowMeterMapper.findActiveByChildId(10L)).thenReturn(List.of(relation));
        when(meterReadingMapper.getLatestByMeterId(10L)).thenReturn(reading(10L, "15"));
        when(priceStrategyMapper.selectById(30L)).thenReturn(fixedStrategy(30L, "1.00", "0"));
        when(customerMapper.subtractBalance(200L, new BigDecimal("10.00"))).thenReturn(1);
        when(customerMapper.selectById(200L)).thenReturn(customer(200L, "90.00"));

        assertThat(service.calculateAndDebit(10L, BigDecimal.ZERO)).isEqualByComparingTo("10.00");

        ArgumentCaptor<AccountFlow> flowCaptor = ArgumentCaptor.forClass(AccountFlow.class);
        verify(accountFlowMapper).insert(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getCustomerId()).isEqualTo(200L);
        assertThat(flowCaptor.getValue().getMeterId()).isEqualTo(99L);

        ArgumentCaptor<MeterOperationTrace> traceCaptor = ArgumentCaptor.forClass(MeterOperationTrace.class);
        verify(meterOperationTraceService).record(traceCaptor.capture());
        assertThat(traceCaptor.getValue().getObjectType()).isEqualTo("VIRTUAL_METER");
        assertThat(traceCaptor.getValue().getMeterId()).isEqualTo(99L);
        assertThat(traceCaptor.getValue().getChildMeterId()).isEqualTo(10L);
        verify(meterMapper).updateLastSettledReading(eq(10L), eq(new BigDecimal("15")), any(LocalDateTime.class));
    }

    @Test
    void settlementAcrossTariffVersionsChargesEachReadingSegmentOnce() {
        Meter meter = meter(10L, 100L, 20L);
        meter.setLastSettledReading(new BigDecimal("100"));
        meter.setLastSettledTime(LocalDateTime.of(2026, 9, 15, 0, 0));
        MeterReading first = readingAt(10L, "110", LocalDateTime.of(2026, 9, 15, 23, 0));
        MeterReading second = readingAt(10L, "120", LocalDateTime.of(2026, 9, 16, 1, 0));
        PriceStrategy bound = fixedStrategy(20L, "0.80", "0");
        bound.setVersionGroupCode("ELEC-2026");
        PriceStrategy oldVersion = fixedStrategy(21L, "0.80", "0");
        oldVersion.setVersionGroupCode("ELEC-2026"); oldVersion.setVersionNo(1);
        PriceStrategy newVersion = fixedStrategy(22L, "0.90", "0");
        newVersion.setVersionGroupCode("ELEC-2026"); newVersion.setVersionNo(2);

        when(meterMapper.selectById(10L)).thenReturn(meter);
        when(shadowMeterMapper.findActiveByChildId(10L)).thenReturn(Collections.emptyList());
        when(meterReadingMapper.getLatestByMeterId(10L)).thenReturn(second);
        when(meterReadingMapper.listForSettlement(10L, meter.getLastSettledTime(), second.getTimestamp()))
                .thenReturn(List.of(first, second));
        when(priceStrategyMapper.selectById(20L)).thenReturn(bound);
        when(priceStrategyMapper.selectById(21L)).thenReturn(oldVersion);
        when(priceStrategyMapper.selectById(22L)).thenReturn(newVersion);
        when(priceStrategyMapper.findEffectiveVersion("ELEC-2026", 1, LocalDate.of(2026, 9, 15)))
                .thenReturn(oldVersion);
        when(priceStrategyMapper.findEffectiveVersion("ELEC-2026", 1, LocalDate.of(2026, 9, 16)))
                .thenReturn(newVersion);
        when(customerMapper.subtractBalance(100L, new BigDecimal("17.00"))).thenReturn(1);
        when(customerMapper.selectById(100L)).thenReturn(customer(100L, "983.00"));

        assertThat(service.calculateAndDebit(10L, BigDecimal.ZERO)).isEqualByComparingTo("17.00");
        verify(customerMapper, times(1)).subtractBalance(100L, new BigDecimal("17.00"));
        ArgumentCaptor<BillingSegment> segments = ArgumentCaptor.forClass(BillingSegment.class);
        verify(billingSegmentMapper, times(2)).insert(segments.capture());
        assertThat(segments.getAllValues()).extracting(BillingSegment::getStrategyId)
                .containsExactly(21L, 22L);
        ArgumentCaptor<MeterOperationTrace> trace = ArgumentCaptor.forClass(MeterOperationTrace.class);
        verify(meterOperationTraceService).record(trace.capture());
        assertThat(trace.getValue().getBillingType()).isEqualTo("MULTI_VERSION");
    }

    @Test
    void tieredPriceContinuesFromVirtualAccountsNaturalMonthUsage() {
        Meter meter = meter(10L, 100L, 30L);
        meter.setLastSettledReading(new BigDecimal("100"));
        MeterReading latest = reading(10L, "120");
        PriceStrategy strategy = fixedStrategy(30L, "0", "0");
        strategy.setPriceType(3);
        strategy.setTimeSlots("[{\"amount\":100,\"price\":1},{\"price\":2}]");

        when(meterMapper.selectById(10L)).thenReturn(meter);
        when(shadowMeterMapper.findActiveByChildId(10L)).thenReturn(Collections.emptyList());
        when(meterReadingMapper.getLatestByMeterId(10L)).thenReturn(latest);
        when(priceStrategyMapper.selectById(30L)).thenReturn(strategy);
        when(billingSegmentMapper.sumPeriodConsumption(eq(10L), any(LocalDateTime.class), eq(latest.getTimestamp())))
                .thenReturn(new BigDecimal("90"));
        when(customerMapper.subtractBalance(100L, new BigDecimal("30.00"))).thenReturn(1);
        when(customerMapper.selectById(100L)).thenReturn(customer(100L, "970.00"));

        assertThat(service.calculateAndDebit(10L, BigDecimal.ZERO)).isEqualByComparingTo("30.00");
        verify(customerMapper).subtractBalance(100L, new BigDecimal("30.00"));
    }

    private Meter meter(Long id, Long customerId, Long strategyId) {
        Meter meter = new Meter();
        meter.setId(id);
        meter.setCustomerId(customerId);
        meter.setMeterKind("PHYSICAL");
        meter.setMeterType(1);
        meter.setRateConfigId(strategyId);
        meter.setCtRatio(BigDecimal.ONE);
        meter.setPtRatio(BigDecimal.ONE);
        return meter;
    }

    private MeterReading reading(Long meterId, String value) {
        return readingAt(meterId, value, LocalDateTime.of(2026, 9, 19, 12, 0));
    }

    private MeterReading readingAt(Long meterId, String value, LocalDateTime timestamp) {
        MeterReading reading = new MeterReading();
        reading.setMeterId(meterId);
        reading.setReadVal(new BigDecimal(value));
        reading.setTimestamp(timestamp);
        return reading;
    }

    private PriceStrategy fixedStrategy(Long id, String fixedPrice, String plusPrice) {
        PriceStrategy strategy = new PriceStrategy();
        strategy.setId(id);
        strategy.setMeterType(1);
        strategy.setPriceType(1);
        strategy.setFixedPrice(new BigDecimal(fixedPrice));
        strategy.setPlusPrice(new BigDecimal(plusPrice));
        strategy.setEffectiveDate(LocalDate.of(2026, 1, 1));
        strategy.setStatus(1);
        return strategy;
    }

    private Customer customer(Long id, String balance) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setBalance(new BigDecimal(balance));
        return customer;
    }
}
