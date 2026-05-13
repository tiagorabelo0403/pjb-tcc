package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalActorRiteTribunalPolicyBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalActorRiteTribunalPolicyTrackFactory {

    private RecursalActorRiteTribunalPolicyTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "MATRIZ_FINA_POLITICA_VISUAL_OPERACIONAL",
                "Matriz fina de política visual e operacional",
                recursoPrincipal,
                RecursalActorRiteTribunalPolicyBlueprint.secoes(recursoPrincipal, request),
                RecursalActorRiteTribunalPolicyBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalActorRiteTribunalPolicyBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
