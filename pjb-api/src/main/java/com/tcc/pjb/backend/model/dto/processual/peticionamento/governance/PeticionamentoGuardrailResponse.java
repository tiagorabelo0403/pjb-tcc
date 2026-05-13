package com.tcc.pjb.backend.model.dto.processual.peticionamento.governance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PeticionamentoGuardrailResponse(
        String status,
        boolean bloqueante,
        boolean alerta,
        boolean prontaParaProtocolar,
        String nextAction,
        List<String> bloqueios,
        List<String> alertas,
        List<String> checklist,
        Map<String, Object> envelope
) {
    public PeticionamentoGuardrailResponse {
        status = normalize(status, "SEM_ANALISE");
        nextAction = normalize(nextAction, "REVISAR_PETICIONAMENTO");
        bloqueios = bloqueios == null ? List.of() : List.copyOf(bloqueios);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        checklist = checklist == null ? List.of() : List.copyOf(checklist);
        envelope = envelope == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(envelope));
    }

    public static PeticionamentoGuardrailResponse vazio() {
        return new PeticionamentoGuardrailResponse("SEM_ANALISE", false, false, false, "REVISAR_PETICIONAMENTO", List.of(), List.of(), List.of(), Map.of());
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("bloqueante", bloqueante);
        out.put("alerta", alerta);
        out.put("prontaParaProtocolar", prontaParaProtocolar);
        out.put("nextAction", nextAction);
        out.put("bloqueios", bloqueios);
        out.put("alertas", alertas);
        out.put("checklist", checklist);
        out.put("envelope", envelope);
        return Map.copyOf(out);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
