package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalAdjudicationWorkbenchBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalAdjudicationWorkbenchTrackFactory {

    private RecursalAdjudicationWorkbenchTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "MALHA_PAINEIS_WORKBENCHES_COMPETENTES",
                "Malha de painéis e workbenches competentes",
                recursoPrincipal,
                RecursalAdjudicationWorkbenchBlueprint.secoes(recursoPrincipal, request),
                RecursalAdjudicationWorkbenchBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalAdjudicationWorkbenchBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
