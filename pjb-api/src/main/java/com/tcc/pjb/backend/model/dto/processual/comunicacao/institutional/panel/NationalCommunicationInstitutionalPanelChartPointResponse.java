package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

public record NationalCommunicationInstitutionalPanelChartPointResponse(
        String label,
        double value,
        String accentColor,
        String tooltip
) {
}
