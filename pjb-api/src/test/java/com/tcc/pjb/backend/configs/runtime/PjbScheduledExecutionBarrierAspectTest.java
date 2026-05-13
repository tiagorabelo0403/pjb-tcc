package com.tcc.pjb.backend.configs.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PjbScheduledExecutionBarrierAspectTest {

    private final PjbRuntimeBarrierProperties properties = new PjbRuntimeBarrierProperties();
    private final PjbScheduledExecutionBarrierAspect aspect = new PjbScheduledExecutionBarrierAspect(properties);

    @Test
    void blocks_ui_when_ui_domain_is_disabled() {
        properties.getScheduling().setUi(false);
        assertTrue(aspect.isBlocked(com.tcc.pjb.backend.service.ui.live.UiHistoryLiveHub.class));
        assertFalse(aspect.isBlocked(com.tcc.pjb.backend.core.scheduler.VitalMonitorScheduler.class));
    }

    @Test
    void blocks_external_integrations_when_external_domain_is_disabled() {
        properties.getScheduling().setIntegracoesExternas(false);
        assertTrue(aspect.isBlocked(com.tcc.pjb.backend.integration.datajud.feed.DataJudFeedScheduler.class));
    }
}
