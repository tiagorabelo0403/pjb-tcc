package com.tcc.pjb.backend.model.dto.processual.runtime.homologation;

import java.time.Instant;
import java.util.List;

public record ProcessualHomologationBlockerDetailResponse(
        String blockerCode,
        String gateCode,
        String categoria,
        String descricao,
        String expedicaoUuid,
        boolean bloqueado,
        Instant createdAt,
        Instant updatedAt,
        List<String> justificativas
) {
    public ProcessualHomologationBlockerDetailResponse {
        justificativas = justificativas == null ? List.of() : List.copyOf(justificativas);
    }
}
