package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalInstitutionalBoxesHistoryBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalInstitutionalBoxesHistoryTrackFactory {

    private RecursalInstitutionalBoxesHistoryTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL",
                "Caixas institucionais e histórico recursal",
                recursoPrincipal,
                RecursalInstitutionalBoxesHistoryBlueprint.secoes(recursoPrincipal, request),
                RecursalInstitutionalBoxesHistoryBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalInstitutionalBoxesHistoryBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
