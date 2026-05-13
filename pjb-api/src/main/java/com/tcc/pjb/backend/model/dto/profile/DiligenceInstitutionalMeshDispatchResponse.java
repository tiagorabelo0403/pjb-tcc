package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.UUID;

public record DiligenceInstitutionalMeshDispatchResponse(
        Long dispatchId,
        String perfil,
        String canal,
        String diligenceReference,
        Long processoId,
        String processoNumero,
        Long annexationId,
        Long juntadaId,
        UUID pacoteDocumentoId,
        UUID outboxEventId,
        String eventType,
        String routingKey,
        String externalSystemCode,
        String destinationBox,
        String meshOrgKey,
        String meshUnitKey,
        String dispatchStatus,
        String replayToken,
        String chainIdempotencyKey,
        String ackProtocol,
        String ackReference,
        String payloadDigestSha256,
        String payloadSignatureHmacSha256,
        Instant deliveredAt,
        Instant acknowledgedAt,
        Instant createdAt
) {
}
