package com.tcc.pjb.backend.model.dto.processual.peticionamento.studio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PeticionamentoStudioGovernedReviewResponse(
        String status,
        String sessionKey,
        String actorProfile,
        String draftingMode,
        @Schema(description = "Contexto procedimental — classeProcessual, ramoDireito, ritoProcessual, justicaSugerida, petitionFamily (Categoria D: passado de projection)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> procedure,
        @Schema(description = "Matriz de gaps documentais — documentos faltantes por tipo (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> documentGapMatrix,
        @Schema(description = "Checklist de protocolo — itens de conformidade procedimental (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> protocolChecklist,
        @Schema(description = "Matriz de risco — checklist, blockingIssues, alerts (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> riskMatrix,
        @Schema(description = "Governança de revisão — regras e aprovadores do processo (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> reviewGovernance,
        List<String> nextSteps
) {
    public PeticionamentoStudioGovernedReviewResponse {
        status = normalize(status, "EM_REVISAO");
        sessionKey = normalize(sessionKey, "SEM_SESSAO");
        actorProfile = normalize(actorProfile, "PETICIONANTE_AUTENTICADO");
        draftingMode = normalize(draftingMode, "REVISAO_GOVERNADA");
        procedure = copyOfMap(procedure);
        documentGapMatrix = copyOfMap(documentGapMatrix);
        protocolChecklist = copyOfMap(protocolChecklist);
        riskMatrix = copyOfMap(riskMatrix);
        reviewGovernance = copyOfMap(reviewGovernance);
        nextSteps = nextSteps == null ? List.of() : List.copyOf(nextSteps);
    }

    private static Map<String, Object> copyOfMap(Map<String, Object> value) {
        return value == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(value));
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}

