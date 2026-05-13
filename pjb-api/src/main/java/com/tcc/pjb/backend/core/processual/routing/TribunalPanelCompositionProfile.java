package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TribunalPanelCompositionProfile(
        String panelCompositionLabel,
        String relatoriaMode,
        String reviewFlow,
        String voteCollectionMode,
        String sustentacaoWindow,
        String publicationSequence,
        String clerkCluster,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public TribunalPanelCompositionProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(panelCompositionLabel, "PANEL"),
                firstNonBlank(relatoriaMode, "RELATORIA"),
                firstNonBlank(voteCollectionMode, "VOTO"),
                firstNonBlank(publicationSequence, "PUBLICACAO"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("panelCompositionLabel", panelCompositionLabel);
        out.put("relatoriaMode", relatoriaMode);
        out.put("reviewFlow", reviewFlow);
        out.put("voteCollectionMode", voteCollectionMode);
        out.put("sustentacaoWindow", sustentacaoWindow);
        out.put("publicationSequence", publicationSequence);
        out.put("clerkCluster", clerkCluster);
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
