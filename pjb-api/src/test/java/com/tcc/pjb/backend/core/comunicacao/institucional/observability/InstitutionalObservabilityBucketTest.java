package com.tcc.pjb.backend.core.comunicacao.institucional.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.domain.InstitutionalObservabilityBucket;

class InstitutionalObservabilityBucketTest {

    @Test
    void shouldExposeKeyAndCount() {
        InstitutionalObservabilityBucket bucket = new InstitutionalObservabilityBucket("PJB_INBOX", 5L);
        assertEquals("PJB_INBOX", bucket.key());
        assertEquals(5L, bucket.count());
    }
}
