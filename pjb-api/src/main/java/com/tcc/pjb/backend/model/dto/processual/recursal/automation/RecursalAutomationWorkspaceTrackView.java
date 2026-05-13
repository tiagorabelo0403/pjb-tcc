package com.tcc.pjb.backend.model.dto.processual.recursal.automation;

import java.util.List;

public record RecursalAutomationWorkspaceTrackView(
        String codigo,
        String titulo,
        String rotaBase,
        List<String> secoesObrigatorias,
        List<RecursalAutomationWorkspaceChecklistItemView> checklistOperacional,
        List<String> alertasTaticos) {
}
