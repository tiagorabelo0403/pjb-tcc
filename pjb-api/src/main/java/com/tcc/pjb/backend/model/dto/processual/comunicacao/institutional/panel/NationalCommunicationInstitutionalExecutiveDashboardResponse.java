package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessContextResponse;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalExecutiveDashboardResponse(
        List<NationalCommunicationInstitutionalPanelCardResponse> cards,
        List<NationalCommunicationInstitutionalPanelNotificationResponse> notifications,
        List<NationalCommunicationInstitutionalPanelProgressStageResponse> progressStages,
        List<NationalCommunicationInstitutionalPanelChartResponse> charts,
        List<NationalCommunicationInstitutionalPanelSummaryResponse> orgaos,
        List<NationalCommunicationInstitutionalUnitQueueResponse> filas,
        NationalCommunicationInstitutionalAccessContextResponse institutionalAccessContext,
        Instant generatedAt
) {
}