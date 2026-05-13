package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPetitioningReuseBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalPetitioningReuseTrackFactory {

    private RecursalPetitioningReuseTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "REUSO_INTELIGENTE_PETICIONAMENTO_RECURSAL",
                "Reuso inteligente do peticionamento recursal e de embargos",
                recursoPrincipal,
                RecursalPetitioningReuseBlueprint.secoes(recursoPrincipal, request),
                RecursalPetitioningReuseBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalPetitioningReuseBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
