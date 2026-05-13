package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalIdentityGuardResponse(
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
}
