package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalProceduralTaxonomyBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalProceduralTaxonomyTrackFactory {

    private RecursalProceduralTaxonomyTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL",
                "Taxonomia processual unificada recursal",
                recursoPrincipal,
                RecursalProceduralTaxonomyBlueprint.secoes(recursoPrincipal, request),
                RecursalProceduralTaxonomyBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalProceduralTaxonomyBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
