package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

public record InstitutionalPanelProgressStage(
        String code,
        String title,
        long total,
        double percentual,
        String accentColor,
        String semanticStatus
) {
}
