package com.tcc.pjb.backend.model.dto.institutional.support.operations;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record InstitutionalSupportPrepautaSnapshotResponse(
        Instant generatedAt,
        @Schema(description = "Snapshot da lane institucional — objeto complexo serializado por tipo de lane (secretaria/distribuicao/arquivo)", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> lane,
        @Schema(description = "Snapshot de processo em pauta — heterogeneo por sistema judicial de origem", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> processo,
        @Schema(description = "Metricas da superficie institucional — chaves numericas variam por tipo de painel", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metrics,
        List<TimelineItem> timeline,
        List<ChecklistItem> checklist,
        List<PendingAct> pendingActs,
        List<DocumentTemplate> projectedDocuments,
        @Schema(description = "Envelope de contato institucional — dados de enderecamento heterogeneos por tipo de orgao", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> contactEnvelope,
        List<String> warnings,
        @Schema(description = "Endpoints da superfície institucional — snapshotPath, agendaPath, competenceMatrixPath, coveragePath, prePautaPath, credentialBasePath, memberPanelPath, frontMode")
        @Size(max = 15)
        Map<String, String> routes
) {
    public record TimelineItem(
            Long workItemId,
            String title,
            String status,
            String queueCode,
            Instant referenceAt,
            boolean blocking,
            List<String> tags
    ) {
    }

    public record ChecklistItem(
            String code,
            String label,
            String status,
            String severity,
            String detail
    ) {
    }

    public record PendingAct(
            String actCode,
            String actLabel,
            String severity,
            Instant dueAt,
            boolean blocking,
            List<String> signals
    ) {
    }

    public record DocumentTemplate(
            String documentCode,
            String title,
            String actAxis,
            String targetPhase,
            boolean sensitive,
            List<String> tags
    ) {
    }
}


