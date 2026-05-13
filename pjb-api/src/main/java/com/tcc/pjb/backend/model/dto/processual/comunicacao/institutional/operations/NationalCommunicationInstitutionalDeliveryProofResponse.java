package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;

public record NationalCommunicationInstitutionalDeliveryProofResponse(
        String proofId,
        String expedicaoUuid,
        Long processoId,
        String etapa,
        String canal,
        Long actorUserId,
        String actorTipoUsuario,
        String evidenciaTipo,
        String evidencia,
        Instant createdAt,
        String hashIntegridade
) {
}
