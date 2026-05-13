package com.tcc.pjb.backend.model.dto.processual.peticionamento.journey;

public record PeticionamentoJourneyActionResponse(
        String code,
        String title,
        String reason,
        boolean automatable,
        String targetLane
) {
    public PeticionamentoJourneyActionResponse {
        code = normalize(code, "ACAO");
        title = normalize(title, code);
        reason = normalize(reason, "Ajuste operacional necessário.");
        targetLane = normalize(targetLane, "GERAL");
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
