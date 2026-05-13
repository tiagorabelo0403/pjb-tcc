package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.Map;

public record PjbFrontendOfficeGovernedPetitionView(
        Long processoId,
        String officeMode,
        Long activeEquipeId,
        String actionType,
        String status,
        Long operationId,
        Long queueItemId,
        Long workItemId,
        String dedupKey,
        boolean queueRequired,
        boolean patronCertificateRequired,
        Long effectiveSignerUserId,
        String effectiveSignerNome,
        String effectiveSignerRegistration,
        String signatureMode,
        boolean signatureEnvelopeReady,
        String signedContentHash,
        String renderedSignedContent,
        Map<String, Object> signatureEnvelope,
        List<String> blockers,
        List<String> warnings
) {
}
