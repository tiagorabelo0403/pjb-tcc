package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalStrongSignaturePolicyResponse(
        String affiliationId,
        String nominationId,
        Long userId,
        String userName,
        String laneCode,
        boolean signOrSubmitCapability,
        boolean managedCredentialActive,
        boolean govBrRequired,
        boolean govBrSatisfied,
        boolean govBrPrataOuroRequired,
        boolean govBrPrataOuroSatisfied,
        boolean qualifiedCertificateRequired,
        boolean qualifiedCertificateSatisfied,
        boolean trustedNetworkOrRemoteAuthorizationRequired,
        boolean trustedNetworkOrRemoteAuthorizationSatisfied,
        boolean mfaRequired,
        boolean mfaSatisfied,
        boolean rootAdministrationApprovalRequired,
        boolean rootAdministrationApprovalSatisfied,
        boolean allowed,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
