package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialAgentMeshService {

    private static final String DEFAULT_OPERATION_NAME = "calculo.judicial.agent-mesh";

    private final PjbExecutionOrchestrator executionOrchestrator;
    private final Duration defaultTimeout;

    public CalculoJudicialAgentMeshService(PjbExecutionOrchestrator executionOrchestrator,
                                           @Value("${pjb.processual.calculo.agent-mesh.timeout:5s}") Duration defaultTimeout) {
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator);
        this.defaultTimeout = sanitize(defaultTimeout);
    }

    public <T> CompletableFuture<T> submit(Supplier<T> supplier) {
        return submit(DEFAULT_OPERATION_NAME, supplier, defaultTimeout, null);
    }

    public <T> CompletableFuture<T> submit(Supplier<T> supplier, Duration timeout, T fallback) {
        return submit(DEFAULT_OPERATION_NAME, supplier, timeout, fallback);
    }

    public <T> CompletableFuture<T> submit(String operationName, Supplier<T> supplier, Duration timeout, T fallback) {
        Objects.requireNonNull(supplier, "supplier");
        Duration effectiveTimeout = sanitize(timeout);
        return executionOrchestrator.supply(
                        PjbExecutionDescriptor.burst(normalizeOperationName(operationName), effectiveTimeout),
                        supplier::get)
                .completeOnTimeout(fallback, effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(ex -> fallback);
    }

    public CompletableFuture<Map<String, Object>> submitMap(Supplier<Map<String, Object>> supplier) {
        return submit(supplier, defaultTimeout, Map.of());
    }

    public void awaitAll(CompletableFuture<?>... futures) {
        awaitAll(defaultTimeout, futures);
    }

    public void awaitAll(Duration timeout, CompletableFuture<?>... futures) {
        if (futures == null || futures.length == 0) {
            return;
        }
        Duration effectiveTimeout = sanitize(timeout).plusMillis(250);
        CompletableFuture<?>[] active = java.util.Arrays.stream(futures)
                .filter(java.util.Objects::nonNull)
                .toArray(CompletableFuture[]::new);
        if (active.length == 0) {
            return;
        }
        try {
            CompletableFuture.allOf(active).get(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            cancelAll(active);
        } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            cancelAll(active);
        }
    }

    public Map<String, Object> meshDescriptor() {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("meshCode", "CALCULO_JUDICIAL_AGENT_MESH");
        descriptor.put("executorBean", "PjbExecutionOrchestrator");
        descriptor.put("concurrencyModel", "bounded_lanes_centralized");
        descriptor.put("governance", "single_execution_mesh_service_via_orchestrator");
        descriptor.put("usagePolicy", "no_virtual_thread_inline_in_domain_services");
        descriptor.put("timeoutMs", defaultTimeout.toMillis());
        return Map.copyOf(descriptor);
    }

    private static String normalizeOperationName(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_OPERATION_NAME;
        }
        return value.trim().replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private static Duration sanitize(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            return Duration.ofSeconds(5);
        }
        return timeout;
    }

    private static void cancelAll(CompletableFuture<?>[] futures) {
        for (CompletableFuture<?> future : futures) {
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
