package com.tcc.pjb.backend.core.security.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class PjbSecurityEventLoggerTest {

    @Test
    void bolaAttempt_incrementaCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PjbSecurityEventLogger logger = new PjbSecurityEventLogger(registry);

        logger.bolaAttempt("TC-001", "user-42", "ACCESS_DENIED");

        assertThat(registry.find("pjb.security.bola.attempt")
                .tag("outcome", "ACCESS_DENIED")
                .counter()).isNotNull();
        assertThat(registry.find("pjb.security.bola.attempt")
                .tag("outcome", "ACCESS_DENIED")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void stepUpDenied_incrementaCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PjbSecurityEventLogger logger = new PjbSecurityEventLogger(registry);

        logger.stepUpDenied("user-1", "OURO", "PRATA");

        assertThat(registry.find("pjb.security.stepup.denied").counter()).isNotNull();
        assertThat(registry.find("pjb.security.stepup.denied").counter().count()).isEqualTo(1.0);
    }

    @Test
    void rateLimitExceeded_incrementaCounterPorCapability() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PjbSecurityEventLogger logger = new PjbSecurityEventLogger(registry);

        logger.rateLimitExceeded("user-99", "laiane_mp_oficio_create");
        logger.rateLimitExceeded("user-99", "laiane_mp_oficio_create");

        assertThat(registry.find("pjb.security.ratelimit.exceeded")
                .tag("capability", "laiane_mp_oficio_create")
                .counter().count()).isEqualTo(2.0);
    }

    @Test
    void idempotencyHit_incrementaCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PjbSecurityEventLogger logger = new PjbSecurityEventLogger(registry);

        logger.idempotencyHit("hash-abc", "TC-002");

        assertThat(registry.find("pjb.security.idempotency.hit").counter()).isNotNull();
        assertThat(registry.find("pjb.security.idempotency.hit").counter().count()).isEqualTo(1.0);
    }
}
