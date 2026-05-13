package com.tcc.pjb.backend.model.dto.consultapublica;

import java.util.List;

public record ConsultaPublicaWorkspaceAccessibilityDto(
        String standard,
        List<String> principles,
        List<String> assistiveFeatures,
        String uiLegendRoute,
        String uiPresentationBundleRoute,
        String uiAccessibilityPreferenceRoute
) {
}
