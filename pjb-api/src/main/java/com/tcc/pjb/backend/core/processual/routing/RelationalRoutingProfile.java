package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RelationalRoutingProfile(
        String linkageMode,
        String preventionMode,
        String dependencyMode,
        String targetReference,
        String distributionModeOverride,
        String deskSuffix,
        String attachmentMode,
        String targetDeskProfile,
        String registryBucket,
        String linkageStrength,
        String triageBucket,
        boolean strictPrevention,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public RelationalRoutingProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveLinkageMode(String fallback) {
        return firstNonBlank(linkageMode, fallback, "AUTONOMA");
    }

    public String effectivePreventionMode(String fallback) {
        return firstNonBlank(preventionMode, fallback, "NENHUM_SINAL");
    }

    public String effectiveDistributionMode(String fallback) {
        return firstNonBlank(distributionModeOverride, fallback, "AUTO_DIRETA");
    }

    public String refineDeskProfile(String fallback) {
        String suffix = firstNonBlank(deskSuffix, "AUTONOMA");
        String base = firstNonBlank(fallback, "SECRETARIA_BASE");
        return base.endsWith('_' + suffix) ? base : base + '_' + suffix;
    }

    public String effectiveDeskProfile(String fallback) {
        return firstNonBlank(targetDeskProfile, refineDeskProfile(fallback), fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("linkageMode", linkageMode);
        out.put("preventionMode", preventionMode);
        out.put("dependencyMode", dependencyMode);
        out.put("targetReference", targetReference);
        out.put("distributionModeOverride", distributionModeOverride);
        out.put("deskSuffix", deskSuffix);
        out.put("attachmentMode", attachmentMode);
        out.put("targetDeskProfile", targetDeskProfile);
        out.put("registryBucket", registryBucket);
        out.put("linkageStrength", linkageStrength);
        out.put("triageBucket", triageBucket);
        out.put("strictPrevention", strictPrevention);
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
