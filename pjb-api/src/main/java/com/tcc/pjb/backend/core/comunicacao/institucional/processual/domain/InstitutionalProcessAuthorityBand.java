package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProcessAuthorityBand(
        String code,
        String title,
        String accentColor,
        boolean enabled,
        boolean sensitive,
        List<String> allowedActions,
        List<String> prohibitedActions,
        List<String> requiredGuards,
        List<String> fundamentos
) {
    public InstitutionalProcessAuthorityBand {
        Objects.requireNonNull(code);
        Objects.requireNonNull(title);
        Objects.requireNonNull(accentColor);
        allowedActions = allowedActions == null ? List.of() : List.copyOf(allowedActions);
        prohibitedActions = prohibitedActions == null ? List.of() : List.copyOf(prohibitedActions);
        requiredGuards = requiredGuards == null ? List.of() : List.copyOf(requiredGuards);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
