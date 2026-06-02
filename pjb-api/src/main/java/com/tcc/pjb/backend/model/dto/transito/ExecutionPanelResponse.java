package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ExecutionPanelResponse(
        Long processoId,
        String numeroProcesso,
        String statusAtual,
        String faseAtual,
        boolean executionReady,
        long pendenciasOperacionais,
        long bloqueiosOperacionais,
        ExecutionPanelSummaryResponse summary,
        List<ExecutionPanelLaneResponse> lanes,
        List<ExecutionPanelActionResponse> suggestedActions,
        @Schema(description = "Estado de integridade do painel de execucao — aggregateId, fingerprint, timestamps e status de reconciliacao", implementation = Object.class)
        @Size(max = 15)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> integrity,
        @Schema(description = "Configuracao de frontend do painel de execucao — endpoints, tabs e preferencias de refresh", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> frontend
) {
    public ExecutionPanelResponse {
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
        suggestedActions = suggestedActions == null ? List.of() : List.copyOf(suggestedActions);
        integrity = integrity == null ? Map.of() : Map.copyOf(integrity);
        frontend = frontend == null ? Map.of() : Map.copyOf(frontend);
    }
}

