package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

class RecursalMeshRetryExecutorTest {

    @Test
    void shouldRetryUntilSuccessForRetriableIndexOperation() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("pjb.recursal.retry.index.max-attempts", "3")
                .withProperty("pjb.recursal.retry.index.initial-backoff-ms", "0")
                .withProperty("pjb.recursal.retry.index.max-backoff-ms", "0");
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) mock(ObjectProvider.class);
        when(telemetryProvider.getIfAvailable()).thenReturn(null);
        RecursalMeshRetryExecutor executor = new RecursalMeshRetryExecutor(environment, telemetryProvider);
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute("index", "batch-save", () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("transient");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void shouldNotRetryForIllegalArgumentException() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("pjb.recursal.retry.notification.max-attempts", "5")
                .withProperty("pjb.recursal.retry.notification.initial-backoff-ms", "0")
                .withProperty("pjb.recursal.retry.notification.max-backoff-ms", "0");
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) mock(ObjectProvider.class);
        when(telemetryProvider.getIfAvailable()).thenReturn(null);
        RecursalMeshRetryExecutor executor = new RecursalMeshRetryExecutor(environment, telemetryProvider);
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> executor.executeVoid("notification", "lawyers", () -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("invalid");
        })).isInstanceOf(IllegalArgumentException.class);
        assertThat(attempts.get()).isEqualTo(1);
    }
}
