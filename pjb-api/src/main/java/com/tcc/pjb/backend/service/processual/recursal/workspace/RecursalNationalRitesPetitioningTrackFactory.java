package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalNationalRitesPetitioningBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalNationalRitesPetitioningTrackFactory {

    private RecursalNationalRitesPetitioningTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL",
                "Matriz nacional de peticionamento recursal por rito e espécie",
                recursoPrincipal,
                RecursalNationalRitesPetitioningBlueprint.secoes(recursoPrincipal, request),
                RecursalNationalRitesPetitioningBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalNationalRitesPetitioningBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
