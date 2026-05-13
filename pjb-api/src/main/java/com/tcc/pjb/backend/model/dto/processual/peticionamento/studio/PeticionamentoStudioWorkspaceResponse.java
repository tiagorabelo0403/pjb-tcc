package com.tcc.pjb.backend.model.dto.processual.peticionamento.studio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PeticionamentoStudioWorkspaceResponse(
        String status,
        String sessionKey,
        String actorProfile,
        String nextAction,
        String draftingMode,
        Map<String, Object> dossier,
        Map<String, Object> procedure,
        Map<String, Object> evidence,
        Map<String, Object> jurisprudence,
        Map<String, Object> caseTimeline,
        Map<String, Object> proofRequestMatrix,
        Map<String, Object> protocolChecklist,
        Map<String, Object> riskMatrix,
        Map<String, Object> documentGapMatrix,
        Map<String, Object> reviewGovernance,
        Map<String, Object> assembly,
        List<String> nextSteps
) {
    public PeticionamentoStudioWorkspaceResponse {
        status = normalize(status, "EM_EDICAO");
        sessionKey = normalize(sessionKey, "SEM_SESSAO");
        actorProfile = normalize(actorProfile, "PETICIONANTE_AUTENTICADO");
        nextAction = normalize(nextAction, "REVISAR_DOSSIE_E_MONTAR_MINUTA");
        draftingMode = normalize(draftingMode, "RAPIDO_ASSISTIDO");
        dossier = copyOfMap(dossier);
        procedure = copyOfMap(procedure);
        evidence = copyOfMap(evidence);
        jurisprudence = copyOfMap(jurisprudence);
        caseTimeline = copyOfMap(caseTimeline);
        proofRequestMatrix = copyOfMap(proofRequestMatrix);
        protocolChecklist = copyOfMap(protocolChecklist);
        riskMatrix = copyOfMap(riskMatrix);
        documentGapMatrix = copyOfMap(documentGapMatrix);
        reviewGovernance = copyOfMap(reviewGovernance);
        assembly = copyOfMap(assembly);
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
