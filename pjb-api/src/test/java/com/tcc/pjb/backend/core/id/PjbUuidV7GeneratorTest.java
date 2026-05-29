package com.tcc.pjb.backend.core.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PjbUuidV7GeneratorTest {

    @Test
    void generateCreatesVersion7Rfc4122UniqueTimeOrderedUuid() {
        UUID a = PjbUuidV7Generator.generate();
        UUID b = PjbUuidV7Generator.generate();

        assertEquals(7, (a.getMostSignificantBits() >> 12) & 0xF);
        assertEquals(2, (a.getLeastSignificantBits() >>> 62) & 0x3);
        assertNotEquals(a, b);
        assertTrue(a.toString().compareTo(b.toString()) <= 0);

        long ms = a.getMostSignificantBits() >>> 16;
        long now = System.currentTimeMillis();
        assertTrue(ms > 0 && ms <= now + 1000);
    }
}
