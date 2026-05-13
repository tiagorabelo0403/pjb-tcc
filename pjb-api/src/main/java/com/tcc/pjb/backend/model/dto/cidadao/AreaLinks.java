package com.tcc.pjb.backend.model.dto.cidadao;

public record AreaLinks(
        String uiLegendUrl,
        String uiAccessibilityPreferenceUrl,
        String uiReadingPreferenceUrl,
        String uiPresentationBundleUrl,
        String chatUrl,
        String chatByProcessoUrlTemplate
) {
}
