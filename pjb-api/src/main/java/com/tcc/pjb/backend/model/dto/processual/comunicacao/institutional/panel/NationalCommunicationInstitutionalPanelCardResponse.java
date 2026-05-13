package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

public record NationalCommunicationInstitutionalPanelCardResponse(
        String code,
        String title,
        long value,
        String subtitle,
        String accentColor,
        String trend,
        String icon,
        String navigationPath
) {
}
