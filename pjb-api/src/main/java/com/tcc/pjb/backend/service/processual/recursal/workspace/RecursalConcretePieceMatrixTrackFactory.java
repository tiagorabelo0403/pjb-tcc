package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalConcretePieceMatrixBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalConcretePieceMatrixTrackFactory {

    private RecursalConcretePieceMatrixTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO",
                "Matriz concreta de peças por ator e por rito",
                recursoPrincipal,
                RecursalConcretePieceMatrixBlueprint.secoes(recursoPrincipal, request),
                RecursalConcretePieceMatrixBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalConcretePieceMatrixBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
