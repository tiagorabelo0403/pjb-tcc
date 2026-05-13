package com.tcc.pjb.backend.model.dto.processual.peticionamento.journey;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PeticionamentoJourneyIntelligenceResponse(
        Instant generatedAt,
        String rito,
        String ramo,
        String currentPhase,
        String operationalPulse,
        int completionScore,
        int completedSteps,
        int totalSteps,
        boolean lowOverheadMode,
        boolean passiveObservation,
        boolean readyForAssistedProtocol,
        boolean readyForRealConnector,
        boolean stepUpRequired,
        boolean certificateRequired,
        boolean autoAdvanceEligible,
        List<String> observedSignals,
        List<String> missingDomains,
        List<PeticionamentoJourneyStepResponse> steps,
        List<PeticionamentoJourneyActionResponse> nextActions,
        Map<String, Object> compactMetrics
) {
    public PeticionamentoJourneyIntelligenceResponse {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        rito = normalize(rito, "COMUM_ORDINARIO");
        ramo = normalize(ramo, "CIVIL");
        currentPhase = normalize(currentPhase, "TRIAGEM");
        operationalPulse = normalize(operationalPulse, "ARRANQUE");
        completionScore = Math.max(0, Math.min(100, completionScore));
        completedSteps = Math.max(0, completedSteps);
        totalSteps = Math.max(completedSteps, totalSteps);
        observedSignals = observedSignals == null ? List.of() : List.copyOf(observedSignals);
        missingDomains = missingDomains == null ? List.of() : List.copyOf(missingDomains);
        steps = steps == null ? List.of() : List.copyOf(steps);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
        compactMetrics = copyOfMap(compactMetrics);
    }

    private static Map<String, Object> copyOfMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        value.forEach((key, entry) -> {
            if (key != null) {
                out.put(key, entry);
            }
        });
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
