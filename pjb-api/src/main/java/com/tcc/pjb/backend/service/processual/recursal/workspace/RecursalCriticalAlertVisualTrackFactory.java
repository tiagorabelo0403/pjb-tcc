package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalCriticalAlertVisualBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalCriticalAlertVisualTrackFactory {

    private RecursalCriticalAlertVisualTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "ALERTA_VERMELHO_MULTICANAL_E_VOTOS_VIVOS",
                "Alerta vermelho multicanal e votos vivos",
                recursoPrincipal,
                RecursalCriticalAlertVisualBlueprint.secoes(recursoPrincipal, request),
                RecursalCriticalAlertVisualBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalCriticalAlertVisualBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
