package com.competition.energycontrollerplatform.service.impl;

import com.competition.energycontrollerplatform.integration.dto.JichengDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class JichengDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesObservedMeterReadingResponse() throws Exception {
        String json = """
                {"data":{"all":[{
                  "customer_code":"project-075",
                  "read_date":"2026-08-23 01:00:01",
                  "read_val":"3847.0000",
                  "read_jval":null,
                  "sign":"project",
                  "steal_no":"masked-meter",
                  "vender":"3378"
                }]}}
                """;

        JichengDTO.MeterReadingResponse result =
                objectMapper.readValue(json, JichengDTO.MeterReadingResponse.class);

        assertThat(result.getData().getAll()).hasSize(1);
        JichengDTO.ReadingItem row = result.getData().getAll().get(0);
        assertThat(row.getCustomerCode()).isEqualTo("project-075");
        assertThat(row.getStealNo()).isEqualTo("masked-meter");
        assertThat(row.getReadDate()).isEqualTo("2026-08-23 01:00:01");
        assertThat(row.getReadVal()).isEqualByComparingTo(new BigDecimal("3847.0000"));
    }
}
