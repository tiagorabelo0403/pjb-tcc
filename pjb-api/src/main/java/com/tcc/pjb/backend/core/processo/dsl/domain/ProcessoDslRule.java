package com.tcc.pjb.backend.core.processo.dsl.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ProcessoDslRule(
        String code,
        String axis,
        String title,
        String expression,
        String effect,
        List<String> allowedProfiles,
        List<String> guards,
        boolean blocking,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil
) {
    public ProcessoDslRule {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        axis = axis == null ? "GERAL" : axis;
        expression = expression == null ? "true" : expression;
        effect = effect == null ? "ALLOW" : effect;
        allowedProfiles = allowedProfiles == null ? List.of() : List.copyOf(allowedProfiles);
        guards = guards == null ? List.of() : List.copyOf(guards);
        effectiveFrom = effectiveFrom == null ? LocalDate.of(2026, 1, 1) : effectiveFrom;
    }
}
