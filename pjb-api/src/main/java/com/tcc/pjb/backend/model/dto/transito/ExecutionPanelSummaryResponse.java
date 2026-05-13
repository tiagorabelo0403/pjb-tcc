package com.tcc.pjb.backend.model.dto.transito;

import java.math.BigDecimal;

public record ExecutionPanelSummaryResponse(
        String speciesCode,
        String currentStage,
        String currentQueue,
        String currentInbox,
        String currentImpact,
        String currentAssetKind,
        String currentGateway,
        String externalStatus,
        String terminalDisposition,
        String satisfactionState,
        BigDecimal satisfactionPercent,
        BigDecimal residualAmount,
        int incidentCount,
        int enforcementCount
) {
}
