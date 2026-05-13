package com.tcc.pjb.backend.model.dto.profile;

public record DiligenceInstitutionalMeshReplayRequest(
        Long originalDispatchId,
        String replayReason,
        String routingKey,
        String idempotencyKey,
        String observacoes
) {
}
