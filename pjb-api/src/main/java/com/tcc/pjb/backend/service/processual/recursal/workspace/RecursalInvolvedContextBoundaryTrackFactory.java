package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalInvolvedContextBoundaryBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalInvolvedContextBoundaryTrackFactory {

    private RecursalInvolvedContextBoundaryTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "FRONTEIRA_ENVOLVIMENTO_E_BUSCA_NEUTRA",
                "Fronteira de envolvimento e busca neutra",
                recursoPrincipal,
                RecursalInvolvedContextBoundaryBlueprint.secoes(recursoPrincipal, request),
                RecursalInvolvedContextBoundaryBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalInvolvedContextBoundaryBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
