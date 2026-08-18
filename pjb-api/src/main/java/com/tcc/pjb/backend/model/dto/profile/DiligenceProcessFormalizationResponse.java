package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.UUID;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;

public record DiligenceProcessFormalizationResponse(
        Long formalizacaoId,
        String actor,
        String canal,
        String diligenciaReferencia,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Long encerramentoId,
        Long certidaoId,
        Long checkpointEventId,
        Long movimentacaoId,
        Long movimentacaoEventSeq,
        UUID minutaDocumentoId,
        Long minutaEventSeq,
        String minutaTitulo,
        String minutaSha256,
        String certidaoDigestSha256,
        String evidenceChaveCustodia,
        Boolean evidenceIntegrityOk,
        Integer documentosReferenciados,
        String idempotencyKey,
        String formalizationDigestSha256,
        Instant createdAt,
        QualifiedSignatureMetadata assinaturaQualificada,
        SovereignValidationResult validacaoSoberana
) {}
