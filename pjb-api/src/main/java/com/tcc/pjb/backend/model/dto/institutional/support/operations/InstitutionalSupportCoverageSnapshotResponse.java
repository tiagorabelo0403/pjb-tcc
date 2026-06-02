package com.tcc.pjb.backend.model.dto.institutional.support.operations;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record InstitutionalSupportCoverageSnapshotResponse(
        Instant generatedAt,
        @Schema(description = "Snapshot da lane institucional — objeto complexo serializado por tipo de lane (secretaria/distribuicao/arquivo)", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> lane,
        @Schema(description = "Metricas da superficie institucional — chaves numericas variam por tipo de painel", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metrics,
        List<Cell> cells,
        List<String> gaps,
        List<String> warnings,
        @Schema(description = "Endpoints da superfície institucional — snapshotPath, agendaPath, competenceMatrixPath, coveragePath, prePautaPath, credentialBasePath, memberPanelPath, frontMode")
        @Size(max = 15)
        Map<String, String> routes
) {
    public record Cell(
            String ownerCode,
            String ownerLabel,
            long totalItems,
            long overdueItems,
            long blockingItems,
            long unassignedItems,
            String loadBand,
            Instant nextDueAt,
            List<String> substitutePool,
            List<String> redistributionSuggestions,
            @Schema(description = "Metricas da superficie institucional — chaves numericas variam por tipo de painel", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metrics
    ) {
    }
}


