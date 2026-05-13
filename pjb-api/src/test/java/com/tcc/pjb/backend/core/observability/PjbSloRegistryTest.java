package com.tcc.pjb.backend.core.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class PjbSloRegistryTest {

    @Test
    void shouldRegisterCriticalOperationTimers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PjbSloRegistry sloRegistry = new PjbSloRegistry(registry);
        sloRegistry.registerSlos();
        assertThat(sloRegistry.timer("peticionamento")).isNotNull();
        assertThat(sloRegistry.timer("mni_remessa")).isNotNull();
    }
}
