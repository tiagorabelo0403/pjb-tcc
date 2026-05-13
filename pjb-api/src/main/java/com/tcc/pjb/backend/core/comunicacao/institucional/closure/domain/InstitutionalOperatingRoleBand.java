package com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain;

import java.util.List;

public record InstitutionalOperatingRoleBand(
        String bandKey,
        String laneKind,
        String nominationRole,
        String tipoUsuario,
        String displayName,
        long activeNominations,
        boolean judicialAuthority,
        boolean institutionalOnly,
        boolean personalDirectEntryAllowed,
        List<String> capacities,
        List<String> fundamentos
) {
    public InstitutionalOperatingRoleBand {
        capacities = capacities == null ? List.of() : List.copyOf(capacities);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
