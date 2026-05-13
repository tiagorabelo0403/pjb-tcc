package com.tcc.pjb.backend.model.dto.processual.peticionamento.studio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PeticionamentoStudioQuickDraftResponse(
        String status,
        String sessionKey,
        String actorProfile,
        String title,
        String draftingMode,
        String markdown,
        Map<String, Object> procedure,
        Map<String, Object> evidence,
        Map<String, Object> caseTimeline,
        Map<String, Object> proofRequestMatrix,
        Map<String, Object> protocolChecklist,
        Map<String, Object> riskMatrix,
        Map<String, Object> documentGapMatrix,
        Map<String, Object> reviewGovernance,
        List<String> checklist,
        List<String> nextSteps
) {
    public PeticionamentoStudioQuickDraftResponse {
        status = normalize(status, "EM_EDICAO");
        sessionKey = normalize(sessionKey, "SEM_SESSAO");
        actorProfile = normalize(actorProfile, "PETICIONANTE_AUTENTICADO");
        title = normalize(title, "PETIÇÃO RÁPIDA");
        draftingMode = normalize(draftingMode, "RAPIDO_ASSISTIDO");
        markdown = normalize(markdown, "# Petição em elaboração\n");
        procedure = copyOfMap(procedure);
        evidence = copyOfMap(evidence);
        caseTimeline = copyOfMap(caseTimeline);
        proofRequestMatrix = copyOfMap(proofRequestMatrix);
        protocolChecklist = copyOfMap(protocolChecklist);
        riskMatrix = copyOfMap(riskMatrix);
        documentGapMatrix = copyOfMap(documentGapMatrix);
        reviewGovernance = copyOfMap(reviewGovernance);
        checklist = checklist == null ? List.of() : List.copyOf(checklist);
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
