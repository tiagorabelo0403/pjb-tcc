package com.tcc.pjb.backend.model.dto.processual.painel.contextual;

import java.util.List;

public record ProcessoPainelContextualWidgetResponse(
        String code,
        String title,
        String kind,
        String status,
        String accentColor,
        String headline,
        String subtitle,
        List<String> insights,
        String navigationPath
) {
    public ProcessoPainelContextualWidgetResponse {
        insights = insights == null ? List.of() : List.copyOf(insights);
    }
}
