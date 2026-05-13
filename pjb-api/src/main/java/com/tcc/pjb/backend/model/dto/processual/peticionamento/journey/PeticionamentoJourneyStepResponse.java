package com.tcc.pjb.backend.model.dto.processual.peticionamento.journey;

public record PeticionamentoJourneyStepResponse(
        int orderIndex,
        String code,
        String lane,
        String title,
        String status,
        boolean automatable,
        int weight
) {
    public PeticionamentoJourneyStepResponse {
        code = normalize(code, "PASSO");
        lane = normalize(lane, "GERAL");
        title = normalize(title, code);
        status = normalize(status, "PENDENTE");
        weight = Math.max(1, weight);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
