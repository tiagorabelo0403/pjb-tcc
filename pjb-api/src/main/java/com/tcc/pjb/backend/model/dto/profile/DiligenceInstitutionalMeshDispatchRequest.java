package com.tcc.pjb.backend.model.dto.profile;

public record DiligenceInstitutionalMeshDispatchRequest(
        Long annexationId,
        String externalSystemCode,
        String destinationBox,
        String meshOrgKey,
        String meshUnitKey,
        String routingKey,
        String idempotencyKey,
        String observacoes
) {
}
