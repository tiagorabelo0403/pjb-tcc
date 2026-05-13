package com.tcc.pjb.backend.model.dto.profile;

public record DiligenceInstitutionalMeshAckRequest(
        String ackProtocol,
        String ackReference,
        Boolean definitiveReceipt,
        String observacoes
) {
}
