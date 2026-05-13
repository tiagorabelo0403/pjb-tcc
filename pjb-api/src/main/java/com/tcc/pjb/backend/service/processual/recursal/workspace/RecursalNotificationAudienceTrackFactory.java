package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalNotificationAudienceBlueprint;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceChecklistItemView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationWorkspaceTrackView;
import java.util.List;

public final class RecursalNotificationAudienceTrackFactory {

    private RecursalNotificationAudienceTrackFactory() {
    }

    public static RecursalAutomationWorkspaceTrackView buildTrack(String recursoPrincipal, RecursalAutomationRequest request) {
        return new RecursalAutomationWorkspaceTrackView(
                "ESCALONAMENTO_ALERTAS_POR_PERFIL",
                "Escalonamento de alertas por perfil",
                recursoPrincipal,
                RecursalNotificationAudienceBlueprint.secoes(recursoPrincipal, request),
                RecursalNotificationAudienceBlueprint.checklist(recursoPrincipal, request).entrySet().stream()
                        .map(entry -> new RecursalAutomationWorkspaceChecklistItemView(entry.getKey(), entry.getValue(), true))
                        .toList(),
                List.copyOf(RecursalNotificationAudienceBlueprint.alertas(recursoPrincipal, request))
        );
    }
}
