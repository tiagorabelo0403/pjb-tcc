package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalContextActivationResponse(
        Long userId,
        String userName,
        String identityCode,
        String affiliationId,
        String nominationId,
        String unidadeCodigo,
        String caixaCodigo,
        boolean personalIdentityAuthenticated,
        boolean institutionalBindingValid,
        boolean operationalContextActive,
        boolean requiresStepUp,
        boolean requiresManualApproval,
        boolean blocked,
        boolean allowed,
        List<String> findings,
        List<String> fundamentos,
        Instant checkedAt
) {
}
