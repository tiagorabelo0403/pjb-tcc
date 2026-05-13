package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CollegiateOrganCatalogProfile(
        String tribunalMacroFamily,
        String secretariatDesk,
        String gabineteCluster,
        String presidencyChannel,
        String compositionHint,
        String sessionCadence,
        String quorumLabel,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public CollegiateOrganCatalogProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveSecretariatDesk(String fallback) {
        return firstNonBlank(secretariatDesk, fallback);
    }

    public String effectiveGabineteCluster(String fallback) {
        return firstNonBlank(gabineteCluster, fallback);
    }

    public String effectiveCompositionHint(String fallback) {
        return firstNonBlank(compositionHint, fallback);
    }

    public String effectiveSessionCadence(String fallback) {
        return firstNonBlank(sessionCadence, fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("tribunalMacroFamily", tribunalMacroFamily);
        out.put("secretariatDesk", secretariatDesk);
        out.put("gabineteCluster", gabineteCluster);
        out.put("presidencyChannel", presidencyChannel);
        out.put("compositionHint", compositionHint);
        out.put("sessionCadence", sessionCadence);
        out.put("quorumLabel", quorumLabel);
        out.put("warnings", warnings);
        out.put("fundamentos", fundamentos);
        out.put("reviewChecklist", reviewChecklist);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
