package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalGateStateResponse(
        String gateStateId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String gateCode,
        String status,
        boolean bloqueado,
        String motivo,
        String ultimaProvaTipo,
        Instant createdAt,
        Instant updatedAt,
        Instant releasedAt,
        List<String> justificativas,
        String hashIntegridade
) {
}
