package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalExpeditionAndProceduralActsBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalExpeditionAndProceduralActsTrackFactory {

    private RecursalExpeditionAndProceduralActsTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "MALOTES_PETICIONAMENTO_ATOS_RECURSAIS",
                "Malotes, peticionamento e atos recursais",
                recursoPrincipal,
                RecursalExpeditionAndProceduralActsBlueprint.secoes(recursoPrincipal, request),
                RecursalExpeditionAndProceduralActsBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalExpeditionAndProceduralActsBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
