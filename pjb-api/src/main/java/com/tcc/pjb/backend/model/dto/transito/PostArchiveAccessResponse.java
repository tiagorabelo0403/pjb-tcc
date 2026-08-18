package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

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
        @Schema(description = "Metadados de acesso pos-transito — heterogeneos por fase de arquivamento", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public PostArchiveAccessResponse {
        fundamentosAcesso = fundamentosAcesso == null ? List.of() : List.copyOf(fundamentosAcesso);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

