package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;

public record DiligenceInstitutionalMeshAckResponse(
        Long dispatchId,
        String canal,
        String diligenceReference,
        String dispatchStatus,
        String ackProtocol,
        String ackReference,
        Instant deliveredAt,
        Instant acknowledgedAt
) {
}
