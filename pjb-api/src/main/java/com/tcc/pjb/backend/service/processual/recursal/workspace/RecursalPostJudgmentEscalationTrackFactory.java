package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPostJudgmentEscalationBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalPostJudgmentEscalationTrackFactory {

    private RecursalPostJudgmentEscalationTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "POS_JULGAMENTO_RECURSAL_ESCALONADO",
                "Pós-julgamento recursal escalonado",
                recursoPrincipal,
                RecursalPostJudgmentEscalationBlueprint.secoes(recursoPrincipal, request),
                RecursalPostJudgmentEscalationBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalPostJudgmentEscalationBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
