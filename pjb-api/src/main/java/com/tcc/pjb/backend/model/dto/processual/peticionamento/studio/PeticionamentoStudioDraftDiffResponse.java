package com.tcc.pjb.backend.model.dto.processual.peticionamento.studio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PeticionamentoStudioDraftDiffResponse(
        String status,
        String sessionKey,
        String actorProfile,
        String title,
        String draftingMode,
        @Schema(description = "Contexto procedimental — classeProcessual, ramoDireito, ritoProcessual, justicaSugerida, petitionFamily (Categoria D: passado de projection)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> procedure,
        @Schema(description = "Resumo do diff entre versões do draft — alterações por seção (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> diffSummary,
        @Schema(description = "Governança de revisão — regras e aprovadores do processo (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> reviewGovernance,
        @Schema(description = "Matriz de risco — checklist, blockingIssues, alerts (Categoria D)")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> riskMatrix,
        List<String> nextSteps
) {
    public PeticionamentoStudioDraftDiffResponse {
        status = normalize(status, "EM_COMPARACAO");
        sessionKey = normalize(sessionKey, "SEM_SESSAO");
        actorProfile = normalize(actorProfile, "PETICIONANTE_AUTENTICADO");
        title = normalize(title, "DIFF DE MINUTA");
        draftingMode = normalize(draftingMode, "REVISAO_GOVERNADA");
        procedure = copyOfMap(procedure);
        diffSummary = copyOfMap(diffSummary);
        reviewGovernance = copyOfMap(reviewGovernance);
        riskMatrix = copyOfMap(riskMatrix);
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

