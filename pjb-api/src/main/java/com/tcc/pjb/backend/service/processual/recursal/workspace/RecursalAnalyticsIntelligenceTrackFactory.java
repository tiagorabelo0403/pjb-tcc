package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalAnalyticsIntelligenceBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalAnalyticsIntelligenceTrackFactory {

    private RecursalAnalyticsIntelligenceTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "OBSERVABILIDADE_INDEXACAO_INTELIGENTE_RECURSAL",
                "Observabilidade, indexação e inteligência recursal",
                recursoPrincipal,
                RecursalAnalyticsIntelligenceBlueprint.secoes(recursoPrincipal, request),
                RecursalAnalyticsIntelligenceBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalAnalyticsIntelligenceBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
