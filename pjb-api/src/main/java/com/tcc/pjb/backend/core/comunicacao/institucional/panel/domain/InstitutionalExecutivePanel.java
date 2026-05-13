package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOrgPanelSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalUnitQueueSummary;
import java.time.Instant;
import java.util.List;

public record InstitutionalExecutivePanel(
        List<InstitutionalPanelCard> cards,
        List<InstitutionalPanelNotification> notifications,
        List<InstitutionalPanelProgressStage> progressStages,
        List<InstitutionalPanelChart> charts,
        List<InstitutionalOrgPanelSummary> orgaos,
        List<InstitutionalUnitQueueSummary> filas,
        Instant generatedAt
) {
}
