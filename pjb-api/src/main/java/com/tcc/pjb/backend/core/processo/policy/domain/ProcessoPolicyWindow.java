package com.tcc.pjb.backend.core.processo.policy.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ProcessoPolicyWindow(
        String code,
        String title,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil,
        boolean active,
        long activeRules,
        List<String> sources
) {
    public ProcessoPolicyWindow {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        effectiveFrom = effectiveFrom == null ? LocalDate.of(2026, 1, 1) : effectiveFrom;
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
