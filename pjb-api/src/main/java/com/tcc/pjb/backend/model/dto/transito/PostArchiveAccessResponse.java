package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;
import java.util.Map;

public record PostArchiveAccessResponse(
        String requestId,
        Long processoId,
        String numeroProcesso,
        boolean autorizado,
        String requesterProfile,
        String visibilityMode,
        boolean reviewRequired,
        String scope,
        List<String> fundamentosAcesso,
        List<String> alertas,
        Map<String, Object> metadata
) {
    public PostArchiveAccessResponse {
        fundamentosAcesso = fundamentosAcesso == null ? List.of() : List.copyOf(fundamentosAcesso);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
