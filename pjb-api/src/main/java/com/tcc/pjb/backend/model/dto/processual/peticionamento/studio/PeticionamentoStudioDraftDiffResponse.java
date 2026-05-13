package com.tcc.pjb.backend.model.dto.processual.peticionamento.studio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PeticionamentoStudioDraftDiffResponse(
        String status,
        String sessionKey,
        String actorProfile,
        String title,
        String draftingMode,
        Map<String, Object> procedure,
        Map<String, Object> diffSummary,
        Map<String, Object> reviewGovernance,
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
