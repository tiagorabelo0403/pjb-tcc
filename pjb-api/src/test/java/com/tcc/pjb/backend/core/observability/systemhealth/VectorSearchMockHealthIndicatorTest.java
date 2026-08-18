package com.tcc.pjb.backend.core.observability.systemhealth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardProfile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.env.Environment;

class VectorSearchMockHealthIndicatorTest {

    @Test
    void modeMockEmAmbienteReal_retornaOutOfService() {
        Environment env = mock(Environment.class);
        when(env.getProperty("pjb.ai.vector.mode", "disabled")).thenReturn("mock");

        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(true);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.PROD);

        VectorSearchMockHealthIndicator indicator = new VectorSearchMockHealthIndicator(env, query);
        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("OUT_OF_SERVICE");
        assertThat(health.getDetails()).containsKey("status");
    }

    @Test
    void modeMockEmAmbienteDev_retornaUp() {
        Environment env = mock(Environment.class);
        when(env.getProperty("pjb.ai.vector.mode", "disabled")).thenReturn("mock");

        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(false);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.DEV);

        VectorSearchMockHealthIndicator indicator = new VectorSearchMockHealthIndicator(env, query);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void modeDisabledEmQualquerAmbiente_retornaUp() {
        Environment env = mock(Environment.class);
        when(env.getProperty("pjb.ai.vector.mode", "disabled")).thenReturn("disabled");

        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(true);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.PROD);

        VectorSearchMockHealthIndicator indicator = new VectorSearchMockHealthIndicator(env, query);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }
}
