package com.competition.energycontrollerplatform.integration.service;

import com.competition.energycontrollerplatform.entity.Customer;
import com.competition.energycontrollerplatform.entity.Meter;
import com.competition.energycontrollerplatform.integration.dto.HaopuArchiveSyncResult;
import com.competition.energycontrollerplatform.integration.dto.HaopuDTO;
import com.competition.energycontrollerplatform.mapper.CustomerMapper;
import com.competition.energycontrollerplatform.mapper.HaopuAuditMapper;
import com.competition.energycontrollerplatform.mapper.MeterMapper;
import com.competition.energycontrollerplatform.mapper.MeterReadingMapper;
import com.competition.energycontrollerplatform.mapper.MeterStateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterDataSyncServiceHaopuAuditTest {
    @Mock MeterMapper meterMapper;
    @Mock MeterReadingMapper meterReadingMapper;
    @Mock MeterStateMapper meterStateMapper;
    @Mock CustomerMapper customerMapper;
    @Mock HaopuAuditMapper haopuAuditMapper;
    @InjectMocks MeterDataSyncService service;

    @Test
    void receiveAuditUsesAtomicUpsertAndDoesNotBreakIngestWhenAuditFails() {
        HaopuDTO.ReadingItem item = new HaopuDTO.ReadingItem();
        item.setCollectorCode(11L);
        item.setMeterCode(22L);
        item.setTimestamp(33L);
        item.setReport(true);
        doThrow(new RuntimeException("audit table unavailable")).when(haopuAuditMapper)
                .upsertReceiveStat(11L, 22L, 33L, true);

        assertDoesNotThrow(() -> service.recordHaopuReceive(item));
        verify(haopuAuditMapper).upsertReceiveStat(11L, 22L, 33L, true);
    }

    @Test
    void archiveArrayCountsExistingCreatedAndRejectedItems() {
        HaopuDTO.MeterArchiveItem existingItem = archive(101L);
        HaopuDTO.MeterArchiveItem newItem = archive(102L);
        Meter existing = new Meter();
        existing.setId(1L);
        Customer pending = new Customer();
        pending.setId(9L);

        when(meterMapper.selectOne(any())).thenReturn(existing, null, existing, null);
        when(customerMapper.selectOne(any())).thenReturn(pending);

        HaopuArchiveSyncResult result = service.syncHaopuArchivesWithResult(
                7L, Arrays.asList(existingItem, newItem, null));

        assertEquals(3, result.responseCount());
        assertEquals(2, result.validCount());
        assertEquals(1, result.createdCount());
        assertEquals(1, result.updatedCount());
        assertEquals(1, result.rejectedCount());
        verify(meterMapper).insert(any(Meter.class));
        verify(meterMapper).updateById(existing);
    }

    private HaopuDTO.MeterArchiveItem archive(Long meterCode) {
        HaopuDTO.MeterArchiveItem item = new HaopuDTO.MeterArchiveItem();
        item.setMeterCode(meterCode);
        item.setDeviceType("AC_3");
        return item;
    }
}
