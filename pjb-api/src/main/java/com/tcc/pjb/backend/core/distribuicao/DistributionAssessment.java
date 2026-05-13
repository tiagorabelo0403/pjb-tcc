package com.tcc.pjb.backend.core.distribuicao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record DistributionAssessment(
        DistribuicaoProcessualNacionalEngine.DistribuicaoRequest request,
        com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService.RoutingDecision routing,
        DistributionConstraintSnapshot constraintSnapshot,
        DistributionGovernanceProfile governance,
        String specializedTrack,
        String status,
        String filaDistribuicao,
        String inboxKey,
        String varaDestino,
        String comarcaDestino,
        String trilhoCompetencia,
        int priority,
        long workItemId,
        List<String> alertas,
        List<String> fundamentos,
        List<String> reviewChecklist,
        Map<String, Object> metadata) {

    DistributionAssessment {
        alertas = safeList(alertas);
        fundamentos = safeList(fundamentos);
        reviewChecklist = safeList(reviewChecklist);
        metadata = safeMap(metadata);
    }

    private static List<String> safeList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String item : input) {
            if (item != null && !item.isBlank()) {
                out.add(item.trim());
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static Map<String, Object> safeMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }
}
