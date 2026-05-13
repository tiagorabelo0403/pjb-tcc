package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalRevocationResponse(
        String affiliationId,
        String orgaoSigla,
        String unidadeCodigo,
        Long targetedUserId,
        String targetedUnitCode,
        boolean revokeAffiliation,
        String resultingAffiliationStatus,
        long nominationsRevoked,
        long remainingActiveNominations,
        long remainingActiveAdministrators,
        boolean contextCutImmediately,
        List<String> revokedNominationIds,
        List<String> fundamentos,
        Instant processedAt
) {
}
