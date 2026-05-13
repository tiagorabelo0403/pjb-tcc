package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TerritorialVenueMeshProfile(
        String venueMode,
        String competenceAnchor,
        String primaryCity,
        String primaryForum,
        String secondaryForum,
        String legalForumType,
        String territorialConfidence,
        boolean forumReviewRequired,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public TerritorialVenueMeshProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveMode(String fallback) {
        return firstNonBlank(venueMode, fallback, "INDEFINIDO");
    }

    public String effectivePrimaryCity(String fallback) {
        return firstNonBlank(primaryCity, fallback);
    }

    public String effectivePrimaryForum(String fallback) {
        return firstNonBlank(primaryForum, fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("venueMode", venueMode);
        out.put("competenceAnchor", competenceAnchor);
        out.put("primaryCity", primaryCity);
        out.put("primaryForum", primaryForum);
        out.put("secondaryForum", secondaryForum);
        out.put("legalForumType", legalForumType);
        out.put("territorialConfidence", territorialConfidence);
        out.put("forumReviewRequired", forumReviewRequired);
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
