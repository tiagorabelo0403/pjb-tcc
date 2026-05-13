package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalTribunalDifferentiationBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalTribunalDifferentiationTrackFactory {

    private RecursalTribunalDifferentiationTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "DIFERENCIACAO_POR_TRIBUNAL_RITO_PRAZO",
                "Diferenciação por tribunal, rito, prazo e filtros internos",
                recursoPrincipal,
                RecursalTribunalDifferentiationBlueprint.secoes(recursoPrincipal, request),
                RecursalTribunalDifferentiationBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalTribunalDifferentiationBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
