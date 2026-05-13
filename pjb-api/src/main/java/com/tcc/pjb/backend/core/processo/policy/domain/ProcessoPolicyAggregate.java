package com.tcc.pjb.backend.core.processo.policy.domain;

import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ProcessoPolicyAggregate(
        ProcessoUnificadoIdentity identity,
        LocalDate referenceDate,
        long totalWindows,
        long activeWindows,
        long blockingPolicies,
        List<ProcessoPolicyWindow> windows,
        List<ProcessoPolicyDecision> decisions,
        List<String> invariants,
        Instant generatedAt
) {
    public ProcessoPolicyAggregate {
        Objects.requireNonNull(identity);
        referenceDate = referenceDate == null ? LocalDate.now() : referenceDate;
        windows = windows == null ? List.of() : List.copyOf(windows);
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        invariants = invariants == null ? List.of() : List.copyOf(invariants);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
