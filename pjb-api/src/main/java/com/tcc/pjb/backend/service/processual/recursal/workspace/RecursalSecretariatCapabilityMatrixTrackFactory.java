package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalSecretariatCapabilityMatrixBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalSecretariatCapabilityMatrixTrackFactory {

    private RecursalSecretariatCapabilityMatrixTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU",
                "Matriz de capacidades da secretaria multigrau",
                recursoPrincipal,
                RecursalSecretariatCapabilityMatrixBlueprint.secoes(recursoPrincipal, request),
                RecursalSecretariatCapabilityMatrixBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalSecretariatCapabilityMatrixBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
