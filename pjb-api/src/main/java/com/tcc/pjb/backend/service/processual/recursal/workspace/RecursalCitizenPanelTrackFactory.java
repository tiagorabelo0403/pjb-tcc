package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalCitizenPanelBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalCitizenPanelTrackFactory {

    private RecursalCitizenPanelTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "PAINEL_CIDADAO_RECURSAL_PROPRIO",
                "Painel recursal do cidadão para processos próprios",
                recursoPrincipal,
                RecursalCitizenPanelBlueprint.secoes(recursoPrincipal, request),
                RecursalCitizenPanelBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalCitizenPanelBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
