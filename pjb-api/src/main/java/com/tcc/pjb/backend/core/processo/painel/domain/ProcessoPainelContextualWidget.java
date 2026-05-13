package com.tcc.pjb.backend.core.processo.painel.domain;

import java.util.List;

public record ProcessoPainelContextualWidget(
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
    public ProcessoPainelContextualWidget {
        insights = insights == null ? List.of() : List.copyOf(insights);
    }
}
