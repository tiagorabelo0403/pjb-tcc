package com.tcc.pjb.backend.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PayloadMapsTest {

    @Test
    void ofEntriesIgnoresNullValuesAndPreservesData() {
        Map<String, Object> payload = PayloadMaps.ofEntries(
                "type", "EVENT",
                "threadId", 10L,
                "reason", null,
                "at", "2026-03-08T12:00:00Z");

        assertEquals("EVENT", payload.get("type"));
        assertEquals(10L, payload.get("threadId"));
        assertEquals("2026-03-08T12:00:00Z", payload.get("at"));
        assertFalse(payload.containsKey("reason"));
    }

    @Test
    void ofEntriesRejectsOddArguments() {
        assertThrows(IllegalArgumentException.class, () -> PayloadMaps.ofEntries("type", "EVENT", "threadId"));
    }

    @Test
    void copyWithoutNullsFiltersInvalidEntries() {
        Map<String, Object> payload = PayloadMaps.copyWithoutNulls(Map.of("type", "EVENT", "threadId", 12L));

        assertEquals(2, payload.size());
        assertEquals("EVENT", payload.get("type"));
        assertEquals(12L, payload.get("threadId"));
    }
}
