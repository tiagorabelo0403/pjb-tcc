package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.util.List;

public record PjbContextualPanelCapabilitySet(
        String panelMode,
        String deadlinePolicy,
        String labelPolicy,
        List<String> visibleActions,
        List<String> hiddenActions,
        List<String> validations,
        List<String> warnings
) {
}
