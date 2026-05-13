package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPanelContextSwitchBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalPanelContextSwitchTrackFactory {

    private RecursalPanelContextSwitchTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL",
                "Comutação contextual por painel, rito e tribunal",
                recursoPrincipal,
                RecursalPanelContextSwitchBlueprint.secoes(recursoPrincipal, request),
                RecursalPanelContextSwitchBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalPanelContextSwitchBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
