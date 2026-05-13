package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TribunalSpecificOrganProfile(
        String tribunalFamily,
        String organAlias,
        String publicationDesk,
        String publicationQueue,
        String reviewDesk,
        String internalRouteKey,
        String topologyDescriptor,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public TribunalSpecificOrganProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveOrganAlias(String fallback) {
        return firstNonBlank(organAlias, fallback);
    }

    public String effectivePublicationDesk(String fallback) {
        return firstNonBlank(publicationDesk, fallback);
    }

    public String effectiveReviewDesk(String fallback) {
        return firstNonBlank(reviewDesk, fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("tribunalFamily", tribunalFamily);
        out.put("organAlias", organAlias);
        out.put("publicationDesk", publicationDesk);
        out.put("publicationQueue", publicationQueue);
        out.put("reviewDesk", reviewDesk);
        out.put("internalRouteKey", internalRouteKey);
        out.put("topologyDescriptor", topologyDescriptor);
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
