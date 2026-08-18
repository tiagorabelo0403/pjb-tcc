package com.tcc.pjb.backend.model.dto.institutional.support.panel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record InstitutionalSupportPanelItemResponse(
        Long workItemId,
        Long processoId,
        String numeroProcesso,
        String titulo,
        String status,
        String queueCode,
        String inboxKey,
        String ramoDireito,
        String ritoProcessual,
        String classeProcessual,
        String vara,
        String comarca,
        String uf,
        Instant dueAt,
        Instant updatedAt,
        String principalContatoNome,
        String principalContatoEmail,
        @Schema(description = "Envelope de contato institucional — dados de enderecamento heterogeneos por tipo de orgao", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> contactEnvelope,
        List<String> tags,
        @Schema(description = "Endpoints da superfície institucional — snapshotPath, agendaPath, competenceMatrixPath, coveragePath, prePautaPath, credentialBasePath, memberPanelPath, frontMode")
        @Size(max = 15)
        Map<String, String> routes
) {
}


