package com.tcc.pjb.backend.model.dto.painel;

import java.util.Objects;

public record PainelSharedExperienceResponse(
        String panelCode,
        Object calendar,
        Object deadlines,
        Object colors,
        Object calculator,
        Object reading,
        Object notes
) {
    public PainelSharedExperienceResponse {
        panelCode = panelCode == null || panelCode.isBlank() ? "GERAL" : panelCode.trim();
        calendar = Objects.requireNonNullElse(calendar, Boolean.FALSE);
        deadlines = Objects.requireNonNullElse(deadlines, Boolean.FALSE);
        colors = Objects.requireNonNullElse(colors, Boolean.FALSE);
        calculator = Objects.requireNonNullElse(calculator, Boolean.FALSE);
        reading = Objects.requireNonNullElse(reading, Boolean.FALSE);
        notes = Objects.requireNonNullElse(notes, java.util.List.of());
    }
}
