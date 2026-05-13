package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalStepUpPolicyResponse(
        Long userId,
        String userName,
        String affiliationId,
        String nominationId,
        String sensitiveAct,
        boolean requiresMfa,
        boolean requiresQualifiedCertificate,
        boolean requiresInstitutionalNetwork,
        boolean acceptsRemoteCertificateAuthorization,
        boolean requiresManualApproval,
        boolean blocked,
        List<String> findings,
        List<String> fundamentos,
        Instant checkedAt
) {
}
