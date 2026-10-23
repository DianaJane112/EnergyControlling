package com.competition.energycontrollerplatform.integration.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HaopuReadingItemUnknownFieldTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void capturesUndocumentedFieldsWithoutMisclassifyingDocumentedFields() throws Exception {
        String json = "{\"report\":true,\"collectorCode\":11,\"meterCode\":22,\"timestamp\":33,"
                + "\"ampA\":1.25,\"channel\":\"B\",\"routeNo\":2}";

        HaopuDTO.ReadingItem item = objectMapper.readValue(json, HaopuDTO.ReadingItem.class);

        assertEquals(11L, item.getCollectorCode());
        assertEquals(22L, item.getMeterCode());
        assertFalse(item.getExtraFields().containsKey("ampA"));
        assertEquals("B", item.getExtraFields().get("channel"));
        assertEquals(2, item.getExtraFields().get("routeNo"));
    }

    @Test
    void documentedPayloadHasNoExtraFields() throws Exception {
        String json = "{\"report\":true,\"collectorCode\":11,\"meterCode\":22,\"timestamp\":33,"
                + "\"voltA\":220,\"voltB\":221,\"voltC\":222,\"ampA\":1,\"ampB\":2,\"ampC\":3,"
                + "\"fact\":0.9,\"act\":10,\"react\":2,\"kwh\":100}";

        HaopuDTO.ReadingItem item = objectMapper.readValue(json, HaopuDTO.ReadingItem.class);

        assertEquals(0, item.getExtraFields().size());
    }
}
