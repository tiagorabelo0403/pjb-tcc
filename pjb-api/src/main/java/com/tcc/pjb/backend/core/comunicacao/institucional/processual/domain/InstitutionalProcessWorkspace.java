package com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalProcessWorkspace(
        String profileCode,
        String displayName,
        String panel,
        String processProfile,
        String trustFloor,
        String accentColor,
        String ritoProcessual,
        String faseProcessual,
        String statusProcessual,
        String ramoDireito,
        List<String> tabs,
        List<String> quickFilters,
        List<String> recursosHabilitados,
        List<String> embargosHabilitados,
        List<InstitutionalProcessActionSpec> actions,
        List<InstitutionalProcessQueueSectionSpec> sections,
        List<InstitutionalProcessVisualLaneSpec> visualLanes,
        List<InstitutionalProcessAuthorityBand> authorityBands,
        List<InstitutionalProcessSeparatorSpec> separators,
        List<String> fundamentos
) {
    public InstitutionalProcessWorkspace {
        Objects.requireNonNull(profileCode);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(panel);
        Objects.requireNonNull(processProfile);
        Objects.requireNonNull(trustFloor);
        Objects.requireNonNull(accentColor);
        tabs = tabs == null ? List.of() : List.copyOf(tabs);
        quickFilters = quickFilters == null ? List.of() : List.copyOf(quickFilters);
        recursosHabilitados = recursosHabilitados == null ? List.of() : List.copyOf(recursosHabilitados);
        embargosHabilitados = embargosHabilitados == null ? List.of() : List.copyOf(embargosHabilitados);
        actions = actions == null ? List.of() : List.copyOf(actions);
        sections = sections == null ? List.of() : List.copyOf(sections);
        visualLanes = visualLanes == null ? List.of() : List.copyOf(visualLanes);
        authorityBands = authorityBands == null ? List.of() : List.copyOf(authorityBands);
        separators = separators == null ? List.of() : List.copyOf(separators);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
