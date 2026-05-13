package com.tcc.pjb.backend.model.dto.processual.malha;

import java.util.List;

public record ProcessoMalhaWidgetResponse(
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
    public ProcessoMalhaWidgetResponse {
        insights = insights == null ? List.of() : List.copyOf(insights);
        code = code == null ? "" : code.trim();
        title = title == null ? "" : title.trim();
        kind = kind == null ? "" : kind.trim();
        status = status == null ? "" : status.trim();
        accentColor = accentColor == null ? "" : accentColor.trim();
        headline = headline == null ? "" : headline.trim();
        subtitle = subtitle == null ? "" : subtitle.trim();
        navigationPath = navigationPath == null ? "" : navigationPath.trim();
    }
}
