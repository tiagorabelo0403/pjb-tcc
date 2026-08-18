package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;

public record DiligenceCertificateResponse(
        Long certidaoId,
        String actor,
        String canal,
        String diligenciaReferencia,
        Long workItemId,
        Long processoId,
        String processoNumero,
        Long checkpointEventId,
        String certidaoTipo,
        String titulo,
        String narrativa,
        String certificateDigestSha256,
        String signatureHmacSha256,
        Double latitude,
        Double longitude,
        Double destinoLatitude,
        Double destinoLongitude,
        Double distanceMeters,
        Boolean insideGeofence,
        Integer tentativaSequencia,
        String evidenceChaveCustodia,
        String attemptTrailDigestSha256,
        Instant createdAt,
        QualifiedSignatureMetadata assinaturaQualificada,
        SovereignValidationResult validacaoSoberana
) {}
