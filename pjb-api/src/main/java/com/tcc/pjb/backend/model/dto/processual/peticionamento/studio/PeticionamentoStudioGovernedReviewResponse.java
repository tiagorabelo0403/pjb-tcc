package com.tcc.pjb.backend.model.dto.processual.peticionamento.studio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PeticionamentoStudioGovernedReviewResponse(
        String status,
        String sessionKey,
        String actorProfile,
        String draftingMode,
        Map<String, Object> procedure,
        Map<String, Object> documentGapMatrix,
        Map<String, Object> protocolChecklist,
        Map<String, Object> riskMatrix,
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
