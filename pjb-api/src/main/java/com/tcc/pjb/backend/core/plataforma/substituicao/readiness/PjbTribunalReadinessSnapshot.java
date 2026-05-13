package com.tcc.pjb.backend.core.plataforma.substituicao.readiness;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbTribunalReadinessSnapshot(
        String tribunalCode,
        PjbTribunalReadinessStatus status,
        int totalCapabilities,
        int readyCapabilities,
        int blockedCapabilities,
        int pendingCapabilities,
        List<PjbTribunalReadinessCapability> capabilities,
        List<String> blockers,
        Instant generatedAt
) {
    public PjbTribunalReadinessSnapshot {
        tribunalCode = Objects.toString(tribunalCode, "").trim().toUpperCase();
        status = status == null ? PjbTribunalReadinessStatus.BLOCKED_BY_GOVERNANCE : status;
        totalCapabilities = Math.max(totalCapabilities, 0);
        readyCapabilities = Math.max(readyCapabilities, 0);
        blockedCapabilities = Math.max(blockedCapabilities, 0);
        pendingCapabilities = Math.max(pendingCapabilities, 0);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        blockers = blockers == null ? List.of() : blockers.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
        generatedAt = generatedAt == null ? Instant.EPOCH : generatedAt;
    }

    public double readinessRatio() {
        return totalCapabilities == 0 ? 0.0d : (double) readyCapabilities / totalCapabilities;
    }

    public boolean productionReady() {
        return status == PjbTribunalReadinessStatus.READY_FOR_PRODUCTION;
    }
}
