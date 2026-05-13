package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalBranchSegmentationBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalBranchSegmentationTrackFactory {

    private RecursalBranchSegmentationTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "MALHA_RECURSAL_POR_RAMO_RITO_SIGILO",
                "Malha recursal segmentada por ramo, rito e sigilo",
                recursoPrincipal,
                RecursalBranchSegmentationBlueprint.secoes(recursoPrincipal, request),
                RecursalBranchSegmentationBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalBranchSegmentationBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
