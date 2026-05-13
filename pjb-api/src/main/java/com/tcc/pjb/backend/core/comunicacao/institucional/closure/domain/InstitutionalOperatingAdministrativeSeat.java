package com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain;

import java.util.List;

public record InstitutionalOperatingAdministrativeSeat(
        String code,
        String displayName,
        String laneKind,
        String nominationRole,
        String processProfile,
        String trustFloor,
        boolean managementSeat,
        boolean requiresStepUp,
        boolean requiresCertificate,
        boolean remoteAuthorized,
        List<String> capacities,
        List<String> restrictions,
        List<String> fundamentos
) {
    public InstitutionalOperatingAdministrativeSeat {
        capacities = capacities == null ? List.of() : List.copyOf(capacities);
        restrictions = restrictions == null ? List.of() : List.copyOf(restrictions);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
