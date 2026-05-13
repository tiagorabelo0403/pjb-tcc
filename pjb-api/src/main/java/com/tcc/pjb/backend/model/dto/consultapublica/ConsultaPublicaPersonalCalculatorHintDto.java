package com.tcc.pjb.backend.model.dto.consultapublica;

public record ConsultaPublicaPersonalCalculatorHintDto(
        String domainCode,
        String title,
        String rationale,
        String workspaceRoute,
        String directRoute,
        boolean aiRecommended,
        String modeSuggestion
) {
}
