package com.tcc.pjb.backend.model.dto.intelligence;

import java.util.List;

public record QualifiedThemeAlertResponse(
        Long processoId,
        boolean autoStaySuggested,
        boolean applicationSuggested,
        String recommendedNextStep,
        List<ThemeMatch> matches,
        List<String> fundamentos
) {
    public record ThemeMatch(
            String family,
            String codigo,
            String status,
            double aderencia,
            boolean stayEligible,
            String suggestedAction,
            String ementa,
            String tese,
            List<String> fundamentos
    ) {
    }
}
