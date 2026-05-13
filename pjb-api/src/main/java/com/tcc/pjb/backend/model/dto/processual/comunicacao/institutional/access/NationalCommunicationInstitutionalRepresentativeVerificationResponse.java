package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalRepresentativeVerificationResponse(
        String requestId,
        Long representativeUserId,
        String representativeName,
        String representativeRole,
        String authorityTitle,
        boolean representativeIdentityComplete,
        boolean representativeDocumentValidated,
        boolean institutionalDomainValidated,
        boolean certificateMaterialValidated,
        boolean trustChainValidated,
        boolean dualKeySatisfied,
        boolean homologationReady,
        List<String> findings,
        List<String> fundamentos,
        Instant checkedAt
) {
}
