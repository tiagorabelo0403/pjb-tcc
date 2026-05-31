package com.tcc.pjb.backend.core.observability.systemhealth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.digitalizacao.DigitalizacaoProperties;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardProfile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class OcrDegradedHealthIndicatorTest {

    private static DigitalizacaoProperties props(boolean enabled) {
        return new DigitalizacaoProperties(enabled, 70.0, "por", 20, 60L, 300_000L);
    }

    @Test
    void ocrDisabledEmAmbienteReal_retornaDegraded() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(true);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.PROD);

        OcrDegradedHealthIndicator indicator = new OcrDegradedHealthIndicator(props(false), query);
        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails()).containsKey("degradationReason");
    }

    @Test
    void ocrEnabledEmAmbienteReal_retornaUp() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(true);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.PROD);

        OcrDegradedHealthIndicator indicator = new OcrDegradedHealthIndicator(props(true), query);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void ocrDisabledEmAmbienteDev_retornaUp() {
        MockGuardEnvironmentQuery query = mock(MockGuardEnvironmentQuery.class);
        when(query.isRealEnvironment()).thenReturn(false);
        when(query.activeGuardProfile()).thenReturn(MockGuardProfile.DEV);

        OcrDegradedHealthIndicator indicator = new OcrDegradedHealthIndicator(props(false), query);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }
}
