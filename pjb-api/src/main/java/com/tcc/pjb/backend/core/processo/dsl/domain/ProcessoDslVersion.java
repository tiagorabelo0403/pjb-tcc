package com.tcc.pjb.backend.core.processo.dsl.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ProcessoDslVersion(
        String code,
        String semanticVersion,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil,
        String strategy,
        List<String> axes
) {
    public ProcessoDslVersion {
        Objects.requireNonNull(code);
        semanticVersion = semanticVersion == null ? "2026.1" : semanticVersion;
        effectiveFrom = effectiveFrom == null ? LocalDate.of(2026, 1, 1) : effectiveFrom;
        strategy = strategy == null ? "CATALOGO_VERSIONADO" : strategy;
        axes = axes == null ? List.of() : List.copyOf(axes);
    }
}
