package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TribunalSessionTopologyProfile(
        String sessionBlock,
        String publicationFlow,
        String internalReviewDesk,
        String panelSizeHint,
        String cadenceHint,
        String sessionSecretariatDesk,
        boolean virtualSessionEligible,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public TribunalSessionTopologyProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(sessionBlock, "SESSAO"),
                firstNonBlank(publicationFlow, "PUBLICACAO"),
                firstNonBlank(panelSizeHint, "PAINEL"),
                firstNonBlank(cadenceHint, "CADENCIA"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("sessionBlock", sessionBlock);
        out.put("publicationFlow", publicationFlow);
        out.put("internalReviewDesk", internalReviewDesk);
        out.put("panelSizeHint", panelSizeHint);
        out.put("cadenceHint", cadenceHint);
        out.put("sessionSecretariatDesk", sessionSecretariatDesk);
        out.put("virtualSessionEligible", virtualSessionEligible);
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
