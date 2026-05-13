package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProcessWorkspaceSummary(
        String profileCode,
        String displayName,
        String panel,
        String processProfile,
        String trustFloor,
        String accentColor,
        int totalActions,
        int totalSections,
        int totalAuthorityBands,
        int totalSeparators,
        List<String> tabs,
        List<String> fundamentos
) {
    public InstitutionalProcessWorkspaceSummary {
        Objects.requireNonNull(profileCode);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(panel);
        Objects.requireNonNull(processProfile);
        Objects.requireNonNull(trustFloor);
        Objects.requireNonNull(accentColor);
        tabs = tabs == null ? List.of() : List.copyOf(tabs);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
