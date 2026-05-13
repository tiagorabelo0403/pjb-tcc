package com.tcc.pjb.backend.core.processo.dsl.domain;

import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoDslAggregate(
        ProcessoUnificadoIdentity identity,
        ProcessoDslVersion version,
        long totalRules,
        long blockingRules,
        List<ProcessoDslBlock> blocks,
        List<String> invariants,
        Instant generatedAt
) {
    public ProcessoDslAggregate {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(version);
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
        invariants = invariants == null ? List.of() : List.copyOf(invariants);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
