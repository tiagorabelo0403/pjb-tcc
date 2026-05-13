package com.tcc.pjb.backend.core.processo.posse.domain;

import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoPosseAggregate(
        ProcessoTrabalhoIdentity identity,
        long totalItems,
        long openItems,
        long transitivelyClaimable,
        long findings,
        List<ProcessoPosseItem> items,
        List<String> alerts,
        Instant generatedAt
) {
    public ProcessoPosseAggregate {
        Objects.requireNonNull(identity);
        items = items == null ? List.of() : List.copyOf(items);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
