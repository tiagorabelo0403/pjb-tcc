package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;

public record DiligenceOperationalClosureResponse(
        Long encerramentoId,
        String actor,
        String canal,
        String diligenciaReferencia,
        String outcome,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Long certidaoId,
        Long checkpointEventId,
        String certidaoDigestSha256,
        String workItemStatusFinal,
        Long followupWorkItemId,
        Integer documentosVinculados,
        String idempotencyKey,
        String executionDigestSha256,
        Instant createdAt,
        QualifiedSignatureMetadata assinaturaQualificada,
        SovereignValidationResult validacaoSoberana
) {}
