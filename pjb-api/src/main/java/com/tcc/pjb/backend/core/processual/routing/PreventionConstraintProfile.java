package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PreventionConstraintProfile(
        String relationMode,
        String anchorReference,
        String normalizedReference,
        String preventionFingerprint,
        String dependencyFingerprint,
        String bindingStrength,
        String triageOverride,
        boolean strictLock,
        boolean autoAttachAllowed,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public PreventionConstraintProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveReference(String fallback) {
        return firstNonBlank(anchorReference, fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("relationMode", relationMode);
        out.put("anchorReference", anchorReference);
        out.put("normalizedReference", normalizedReference);
        out.put("preventionFingerprint", preventionFingerprint);
        out.put("dependencyFingerprint", dependencyFingerprint);
        out.put("bindingStrength", bindingStrength);
        out.put("triageOverride", triageOverride);
        out.put("strictLock", strictLock);
        out.put("autoAttachAllowed", autoAttachAllowed);
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
