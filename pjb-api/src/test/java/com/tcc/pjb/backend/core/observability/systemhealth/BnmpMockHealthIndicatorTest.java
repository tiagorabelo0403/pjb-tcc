package com.tcc.pjb.backend.core.observability.systemhealth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.judicial.PjbBnmpProperties;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardProfile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class BnmpMockHealthIndicatorTest {

    private static PjbBnmpProperties props(boolean enabled, boolean mockEnabled) {
        return new PjbBnmpProperties(enabled, mockEnabled, 15);
    }

    @Test
    void mockEnabledEmAmbienteReal_retornaOutOfService() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(true);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.PROD);

        BnmpMockHealthIndicator indicator = new BnmpMockHealthIndicator(props(false, true), query);
        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("OUT_OF_SERVICE");
        assertThat(health.getDetails()).containsKey("status");
    }

    @Test
    void mockEnabledEmAmbienteTeste_retornaUp() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(false);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.TEST);

        BnmpMockHealthIndicator indicator = new BnmpMockHealthIndicator(props(false, true), query);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void mockDisabledEmQualquerAmbiente_retornaUp() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(true);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.PROD);

        BnmpMockHealthIndicator indicator = new BnmpMockHealthIndicator(props(true, false), query);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }
}
