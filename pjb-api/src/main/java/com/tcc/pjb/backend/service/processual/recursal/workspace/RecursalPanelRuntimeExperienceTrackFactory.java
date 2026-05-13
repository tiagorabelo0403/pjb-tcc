package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPanelRuntimeExperienceBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalPanelRuntimeExperienceTrackFactory {

    private RecursalPanelRuntimeExperienceTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal,
                                                                  RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "SHELL_CONTEXTUAL_TATICO_DO_RITO",
                "Shell contextual tático do rito",
                recursoPrincipal,
                RecursalPanelRuntimeExperienceBlueprint.secoes(recursoPrincipal, request),
                RecursalPanelRuntimeExperienceBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalPanelRuntimeExperienceBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
