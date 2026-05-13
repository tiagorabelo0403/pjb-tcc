package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalExternalOperationsPanelBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalExternalOperationsPanelTrackFactory {

    private RecursalExternalOperationsPanelTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "PAINEL_EXTERNO_OPERACIONAL_RECURSAL",
                "Painel externo operacional recursal",
                recursoPrincipal,
                RecursalExternalOperationsPanelBlueprint.secoes(recursoPrincipal, request),
                RecursalExternalOperationsPanelBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalExternalOperationsPanelBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
