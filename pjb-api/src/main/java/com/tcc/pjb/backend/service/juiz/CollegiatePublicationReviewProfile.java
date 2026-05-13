package com.tcc.pjb.backend.service.juiz;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CollegiatePublicationReviewProfile(
        String publicationDesk,
        String reviewDesk,
        String publicationQueue,
        String publicationDeadlineMode,
        String reviewMode,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public CollegiatePublicationReviewProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(publicationDesk, "PUBLICACAO"),
                firstNonBlank(reviewDesk, "REVISAO"),
                firstNonBlank(publicationDeadlineMode, "PRAZO"),
                firstNonBlank(reviewMode, "MODO"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("publicationDesk", publicationDesk);
        out.put("reviewDesk", reviewDesk);
        out.put("publicationQueue", publicationQueue);
        out.put("publicationDeadlineMode", publicationDeadlineMode);
        out.put("reviewMode", reviewMode);
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
