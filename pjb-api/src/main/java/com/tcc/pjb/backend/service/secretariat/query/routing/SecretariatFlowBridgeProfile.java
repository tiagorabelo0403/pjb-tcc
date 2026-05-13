package com.tcc.pjb.backend.service.secretariat.query.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SecretariatFlowBridgeProfile(
        String downstreamAxis,
        String bridgeMode,
        String distributionDesk,
        String gabineteDesk,
        String recursalDesk,
        String admissibilityDesk,
        boolean requiresDistributionSync,
        boolean requiresGabineteSync,
        boolean requiresRecursalSync,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public SecretariatFlowBridgeProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(downstreamAxis, "SECRETARIA"),
                firstNonBlank(bridgeMode, "LOCAL"),
                firstNonBlank(gabineteDesk, "GAB"),
                firstNonBlank(recursalDesk, "REC"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("downstreamAxis", downstreamAxis);
        out.put("bridgeMode", bridgeMode);
        out.put("distributionDesk", distributionDesk);
        out.put("gabineteDesk", gabineteDesk);
        out.put("recursalDesk", recursalDesk);
        out.put("admissibilityDesk", admissibilityDesk);
        out.put("requiresDistributionSync", requiresDistributionSync);
        out.put("requiresGabineteSync", requiresGabineteSync);
        out.put("requiresRecursalSync", requiresRecursalSync);
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
