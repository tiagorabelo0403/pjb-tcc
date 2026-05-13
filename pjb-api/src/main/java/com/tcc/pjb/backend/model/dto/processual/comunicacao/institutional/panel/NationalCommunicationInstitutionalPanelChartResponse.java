package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.util.List;

public record NationalCommunicationInstitutionalPanelChartResponse(
        String chartId,
        String title,
        String chartType,
        String accentColor,
        List<NationalCommunicationInstitutionalPanelChartPointResponse> points
) {
}
