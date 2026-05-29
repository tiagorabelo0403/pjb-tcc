package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.UUID;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;

public record DiligenceAutomaticFilingResponse(
        Long juntadaId,
        String actor,
        String canal,
        String diligenciaReferencia,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Long formalizacaoId,
        Long encerramentoId,
        Long certidaoId,
        UUID minutaDocumentoId,
        UUID pacoteDocumentoId,
        Long movimentacaoId,
        Long movimentacaoEventSeq,
        Long pacoteEventSeq,
        String evidenceChaveCustodia,
        Boolean evidenceIntegrityOk,
        Integer documentosReferenciados,
        String externalSystemCode,
        String bundleReference,
        String bundleDigestSha256,
        String bundleSignatureHmacSha256,
        String idempotencyKey,
        Instant createdAt,
        QualifiedSignatureMetadata assinaturaQualificada,
        SovereignValidationResult validacaoSoberana
) {}
