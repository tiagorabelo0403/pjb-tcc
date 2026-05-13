package com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain;

import java.time.Instant;

public record InstitutionalCommunicationMeshMirrorResult(
        String mirrorId,
        String expedicaoUuid,
        String channel,
        String routingKey,
        String externalSystemCode,
        String destinationBox,
        String meshOrgKey,
        String meshUnitKey,
        String payloadDigestSha256,
        String payloadSignatureHmacSha256,
        String outboxEventId,
        Instant mirroredAt,
        String hashIntegridade
) {
}
