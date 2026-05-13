package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.configs.security.hardening.PjbOperationalCrisisProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PjbOperationalCrisisServiceTest {

    @Test
    void shouldBlockConfiguredPrefixWhenContainmentIsActive() {
        PjbOperationalCrisisProperties properties = new PjbOperationalCrisisProperties();
        properties.setEnabled(true);
        properties.setMode(PjbOperationalCrisisProperties.CrisisMode.CONTAINMENT);
        properties.getBlockedPrefixes().add("/api/v1/jobs");
        PjbOperationalCrisisService service = new PjbOperationalCrisisService(properties);

        PjbOperationalCrisisService.CrisisDecision decision = service.evaluate(
                "/api/v1/jobs/rebuild-index",
                "institutional-bulk",
                96,
                Duration.ofMillis(20),
                503,
                "LOAD_SHED_BULK"
        );

        assertTrue(decision.active());
        assertTrue(decision.blocked());
        assertEquals("containment", decision.mode());
        assertEquals(503, decision.rejectionStatus());
        assertEquals("CRISIS_CONTAINMENT", decision.rejectionCode());
    }

    @Test
    void shouldOverrideLaneBudgetWithoutNullFragility() {
        PjbOperationalCrisisProperties properties = new PjbOperationalCrisisProperties();
        properties.setEnabled(true);
        properties.setMode(PjbOperationalCrisisProperties.CrisisMode.ELEVATED);
        PjbOperationalCrisisProperties.LaneDirective directive = new PjbOperationalCrisisProperties.LaneDirective();
        directive.setName("petition-heavy");
        directive.setMaxInFlightOverride(48);
        directive.setAcquireTimeoutOverride(Duration.ofMillis(5));
        properties.getLaneDirectives().add(directive);
        PjbOperationalCrisisService service = new PjbOperationalCrisisService(properties);

        PjbOperationalCrisisService.CrisisDecision decision = service.evaluate(
                "/api/v1/peticionamento/protocolo",
                "petition-heavy",
                128,
                Duration.ofMillis(25),
                503,
                "LOAD_SHED_PETITION"
        );

        assertTrue(decision.active());
        assertFalse(decision.blocked());
        assertEquals(48, decision.laneLimit());
        assertEquals(Duration.ofMillis(5), decision.laneAcquireTimeout());
        assertEquals("elevated", decision.mode());
    }
}
