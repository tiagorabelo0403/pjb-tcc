package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

public record NationalCommunicationInstitutionalPanelProgressStageResponse(
        String code,
        String title,
        long total,
        double percentual,
        String accentColor,
        String semanticStatus
) {
}
