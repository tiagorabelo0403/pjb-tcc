package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalIdentityGuardDecision(
        Long userId,
        String userName,
        String identityCode,
        String tipoUsuarioBase,
        boolean directPersonalEntryAllowed,
        boolean institutionalAdhesionRequired,
        String preferredEntryMode,
        String preferredPanel,
        String trustFloor,
        boolean requiresInstitutionalNomination,
        List<String> directProfiles,
        List<String> institutionalProfiles,
        List<String> fundamentos,
        Instant checkedAt
) {
    public InstitutionalIdentityGuardDecision {
        directProfiles = directProfiles == null ? List.of() : List.copyOf(directProfiles);
        institutionalProfiles = institutionalProfiles == null ? List.of() : List.copyOf(institutionalProfiles);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
