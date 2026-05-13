package com.tcc.pjb.backend.model.dto.consultapublica;

import java.util.List;

public record ConsultaPublicaPersonalAiAssistDto(
        String processRoute,
        String generalRoute,
        String contextLabel,
        boolean processScoped,
        List<String> suggestedPrompts,
        List<String> guardrails
) {
}
