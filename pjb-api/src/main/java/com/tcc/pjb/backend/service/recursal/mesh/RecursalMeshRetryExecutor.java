package com.tcc.pjb.backend.service.recursal.mesh;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class RecursalMeshRetryExecutor {

    private final Environment environment;
    private final ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider;

    public RecursalMeshRetryExecutor(Environment environment,
                                     ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.telemetryProvider = Objects.requireNonNull(telemetryProvider, "telemetryProvider");
    }

    public void executeVoid(String operation, String target, Runnable action) {
        execute(operation, target, () -> {
            action.run();
            return null;
        });
    }

    public <T> T execute(String operation, String target, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        Policy policy = policyOf(operation);
        RuntimeException last = null;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            int attemptSnapshot = attempt;
            try {
                T result = action.get();
                if (attemptSnapshot > 1) {
                    telemetry().ifPresent(it -> it.recordRetrySuccess(operation, target, attemptSnapshot));
                }
                return result;
            } catch (RuntimeException ex) {
                last = ex;
                if (!policy.enabled() || !isRetriable(ex) || attemptSnapshot >= policy.maxAttempts()) {
                    telemetry().ifPresent(it -> it.recordRetryExhausted(operation, target));
                    throw ex;
                }
                int nextAttemptSnapshot = attemptSnapshot + 1;
                telemetry().ifPresent(it -> it.recordRetryAttempt(operation, target, nextAttemptSnapshot));
                park(policy.backoffForAttempt(attemptSnapshot));
            }
        }
        throw last == null ? new IllegalStateException("Retry executor exhausted without captured exception.") : last;
    }

    private java.util.Optional<RecursalMeshOperationalTelemetryService> telemetry() {
        return java.util.Optional.ofNullable(telemetryProvider.getIfAvailable());
    }

    private Policy policyOf(String operation) {
        String normalized = operation == null || operation.isBlank() ? "default" : operation.trim().toLowerCase(Locale.ROOT);
        boolean enabled = environment.getProperty("pjb.recursal.retry.enabled", Boolean.class, Boolean.TRUE);
        int maxAttempts = environment.getProperty("pjb.recursal.retry." + normalized + ".max-attempts", Integer.class,
                "index".equals(normalized) ? 4 : 3);
        long initialBackoffMs = environment.getProperty("pjb.recursal.retry." + normalized + ".initial-backoff-ms", Long.class,
                "index".equals(normalized) ? 150L : 100L);
        long maxBackoffMs = environment.getProperty("pjb.recursal.retry." + normalized + ".max-backoff-ms", Long.class,
                "index".equals(normalized) ? 2000L : 1000L);
        return new Policy(enabled, Math.max(1, maxAttempts), Math.max(0L, initialBackoffMs), Math.max(0L, maxBackoffMs));
    }

    private boolean isRetriable(RuntimeException ex) {
        return !(ex instanceof IllegalArgumentException)
                && !(ex instanceof UnsupportedOperationException)
                && !(ex instanceof NullPointerException);
    }

    private void park(long backoffMs) {
        if (backoffMs <= 0L) {
            return;
        }
        LockSupport.parkNanos(java.time.Duration.ofMillis(backoffMs).toNanos());
    }

    private record Policy(boolean enabled, int maxAttempts, long initialBackoffMs, long maxBackoffMs) {
        long backoffForAttempt(int previousAttempt) {
            long factor = 1L << Math.min(Math.max(0, previousAttempt - 1), 10);
            long computed;
            try {
                computed = Math.multiplyExact(initialBackoffMs, factor);
            } catch (ArithmeticException overflow) {
                computed = maxBackoffMs;
            }
            long bounded = Math.min(maxBackoffMs, Math.max(0L, computed));
            long jitter = bounded <= 1L ? 0L : ThreadLocalRandom.current().nextLong(Math.max(1L, bounded / 5L));
            return bounded + jitter;
        }
    }
}
