package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FracionaryOrganRoutingProfile(
        String orgaoFracionario,
        String colegiadoMode,
        String chamberFamily,
        String gabineteMode,
        String admissibilityDesk,
        String sessionMode,
        String panelComposition,
        String allocationStrategyOverride,
        String deskProfileOverride,
        boolean virtualSessionEligible,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public FracionaryOrganRoutingProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveOrgaoJulgador(String fallback) {
        return firstNonBlank(orgaoFracionario, fallback);
    }

    public String effectiveAllocationStrategy(String fallback) {
        return firstNonBlank(allocationStrategyOverride, fallback);
    }

    public String effectiveDeskProfile(String fallback) {
        return firstNonBlank(deskProfileOverride, fallback);
    }

    public String effectiveMesaTriagem(String fallback) {
        return firstNonBlank(admissibilityDesk, fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("orgaoFracionario", orgaoFracionario);
        out.put("colegiadoMode", colegiadoMode);
        out.put("chamberFamily", chamberFamily);
        out.put("gabineteMode", gabineteMode);
        out.put("admissibilityDesk", admissibilityDesk);
        out.put("sessionMode", sessionMode);
        out.put("panelComposition", panelComposition);
        out.put("allocationStrategyOverride", allocationStrategyOverride);
        out.put("deskProfileOverride", deskProfileOverride);
        out.put("virtualSessionEligible", virtualSessionEligible);
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
