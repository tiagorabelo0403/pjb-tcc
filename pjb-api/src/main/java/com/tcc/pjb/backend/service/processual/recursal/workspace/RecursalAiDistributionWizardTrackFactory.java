package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalAiDistributionWizardBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalAiDistributionWizardTrackFactory {

    private RecursalAiDistributionWizardTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "WIZARD_DISTRIBUICAO_ASSISTIDA_IA",
                "Wizard de distribuição assistida por IA",
                recursoPrincipal,
                RecursalAiDistributionWizardBlueprint.secoes(recursoPrincipal, request),
                RecursalAiDistributionWizardBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalAiDistributionWizardBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
