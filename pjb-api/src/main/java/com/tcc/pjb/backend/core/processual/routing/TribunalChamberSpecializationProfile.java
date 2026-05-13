package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TribunalChamberSpecializationProfile(
        String chamberLabel,
        String relatoriaDesk,
        String advisoryDesk,
        String preventionClass,
        String distributionPool,
        String sessionRoom,
        String specializationDepth,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public TribunalChamberSpecializationProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveChamberLabel(String fallback) {
        return firstNonBlank(chamberLabel, fallback);
    }

    public String effectiveRelatoriaDesk(String fallback) {
        return firstNonBlank(relatoriaDesk, fallback);
    }

    public String effectiveAdvisoryDesk(String fallback) {
        return firstNonBlank(advisoryDesk, fallback);
    }

    public String effectiveSessionRoom(String fallback) {
        return firstNonBlank(sessionRoom, fallback);
    }

    public String effectivePreventionClass(String fallback) {
        return firstNonBlank(preventionClass, fallback);
    }

    public String effectiveDistributionPool(String fallback) {
        return firstNonBlank(distributionPool, fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("chamberLabel", chamberLabel);
        out.put("relatoriaDesk", relatoriaDesk);
        out.put("advisoryDesk", advisoryDesk);
        out.put("preventionClass", preventionClass);
        out.put("distributionPool", distributionPool);
        out.put("sessionRoom", sessionRoom);
        out.put("specializationDepth", specializationDepth);
        out.put("labels", labels);
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
