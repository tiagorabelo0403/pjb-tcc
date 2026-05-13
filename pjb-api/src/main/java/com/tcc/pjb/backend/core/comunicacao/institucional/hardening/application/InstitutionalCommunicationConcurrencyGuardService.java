package com.tcc.pjb.backend.core.comunicacao.institucional.hardening.application;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain.InstitutionalConcurrentOperationException;
import com.tcc.pjb.backend.platform.cluster.PjbClusterLockService;

@Service
public class InstitutionalCommunicationConcurrencyGuardService {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(45);

    private final PjbClusterLockService clusterLockService;

    public InstitutionalCommunicationConcurrencyGuardService(PjbClusterLockService clusterLockService) {
        this.clusterLockService = Objects.requireNonNull(clusterLockService, "clusterLockService");
    }

    public <T> T execute(String operation, String resourceKey, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        String normalizedKey = normalize(operation, resourceKey);
        PjbClusterLockService.Lease lease = clusterLockService.tryAcquire(normalizedKey, DEFAULT_TTL)
                .orElseThrow(() -> new InstitutionalConcurrentOperationException("Operação institucional concorrente em andamento para " + normalizedKey));
        try (lease) {
            return action.get();
        }
    }

    String normalize(String operation, String resourceKey) {
        String op = operation == null || operation.isBlank() ? "generic" : operation.trim().replace(' ', '-').toLowerCase();
        String key = resourceKey == null || resourceKey.isBlank() ? "global" : resourceKey.trim();
        return "institutional:" + op + ":" + key;
    }
}
