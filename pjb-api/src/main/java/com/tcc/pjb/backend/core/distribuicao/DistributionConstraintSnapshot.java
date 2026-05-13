package com.tcc.pjb.backend.core.distribuicao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DistributionConstraintSnapshot(
        String relationMode,
        String attachmentMode,
        String registryBucket,
        String preventionFingerprint,
        String dependencyFingerprint,
        String bindingStrength,
        String queueSuffix,
        boolean reviewRequired,
        LinkedHashMap<String, Object> metadata) {

    public DistributionConstraintSnapshot {
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return firstNonBlank(relationMode, "AUTONOMA") + ':' + firstNonBlank(bindingStrength, "NORMAL") + ':' + firstNonBlank(queueSuffix, "BASE");
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("relationMode", relationMode);
        out.put("attachmentMode", attachmentMode);
        out.put("registryBucket", registryBucket);
        out.put("preventionFingerprint", preventionFingerprint);
        out.put("dependencyFingerprint", dependencyFingerprint);
        out.put("bindingStrength", bindingStrength);
        out.put("queueSuffix", queueSuffix);
        out.put("reviewRequired", reviewRequired);
        out.put("descriptor", descriptor());
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
