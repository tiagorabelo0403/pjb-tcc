package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalRevocationResult(
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
