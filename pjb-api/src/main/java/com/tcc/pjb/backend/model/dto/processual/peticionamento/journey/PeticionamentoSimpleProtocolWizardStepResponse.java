package com.tcc.pjb.backend.model.dto.processual.peticionamento.journey;

import java.util.List;

public record PeticionamentoSimpleProtocolWizardStepResponse(
        int orderIndex,
        String code,
        String lane,
        String title,
        String status,
        boolean blocking,
        List<String> guidance
) {
    public PeticionamentoSimpleProtocolWizardStepResponse {
        orderIndex = Math.max(orderIndex, 1);
        code = normalize(code, "UNSPECIFIED");
        lane = normalize(lane, "OPERACIONAL");
        title = normalize(title, "Etapa operacional");
        status = normalize(status, "PENDENTE");
        guidance = guidance == null ? List.of() : List.copyOf(guidance);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
