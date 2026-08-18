package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;

public record DiligenceInstitutionalAnnexationResponse(
        Long annexationId,
        String perfil,
        String canal,
        String diligenceReference,
        Long processoId,
        String processoNumero,
        Long juntadaId,
        Long formalizacaoId,
        Long encerramentoId,
        Long certidaoId,
        UUID pacoteDocumentoId,
        String externalSystemCode,
        String destinationBox,
        String ackProtocol,
        String ackReference,
        String annexationStatus,
        String bundleReference,
        String chainIdempotencyKey,
        String executionDigestSha256,
        Long processEventSeq,
        OffsetDateTime externalizedAt,
        Instant createdAt,
        QualifiedSignatureMetadata assinaturaQualificada,
        SovereignValidationResult validacaoSoberana
) {}
