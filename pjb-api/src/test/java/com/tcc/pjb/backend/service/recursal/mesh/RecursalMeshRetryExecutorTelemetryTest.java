package com.tcc.pjb.backend.service.recursal.mesh;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

class RecursalMeshRetryExecutorTelemetryTest {

    @Test
    void shouldRecordRetryAttemptAndSuccessWithStableAttemptSnapshots() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("pjb.recursal.retry.index.max-attempts", "3")
                .withProperty("pjb.recursal.retry.index.initial-backoff-ms", "0")
                .withProperty("pjb.recursal.retry.index.max-backoff-ms", "0");
        RecursalMeshOperationalTelemetryService telemetry = mock(RecursalMeshOperationalTelemetryService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) mock(ObjectProvider.class);
        when(telemetryProvider.getIfAvailable()).thenReturn(telemetry);
        RecursalMeshRetryExecutor executor = new RecursalMeshRetryExecutor(environment, telemetryProvider);
        final int[] attempts = {0};

        executor.execute("index", "batch-save", () -> {
            attempts[0]++;
            if (attempts[0] < 2) {
                throw new IllegalStateException("transient");
            }
            return "ok";
        });

        verify(telemetry).recordRetryAttempt("index", "batch-save", 2);
        verify(telemetry).recordRetrySuccess("index", "batch-save", 2);
        verify(telemetry, never()).recordRetryExhausted("index", "batch-save");
    }
}
