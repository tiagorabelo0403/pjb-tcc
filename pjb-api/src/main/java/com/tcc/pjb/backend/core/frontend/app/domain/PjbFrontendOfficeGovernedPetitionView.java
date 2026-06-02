package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

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
        @Schema(description = "Envelope de assinatura qualificada — estrutura varia por tipo de assinatura e HSM ativo", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> signatureEnvelope,
        List<String> blockers,
        List<String> warnings
) {
}

