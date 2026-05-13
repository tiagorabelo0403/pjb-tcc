package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralCoverageProfile(
        String justiceTrack,
        String tribunalTier,
        String riteFamily,
        String materialityAxis,
        String forumScope,
        String territorialAnchor,
        String admissibilityChannel,
        String executionTrack,
        String recursalTrack,
        String preventionAnchor,
        String concurrencyEnvelope,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public ProceduralCoverageProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(justiceTrack, "JUSTICA"),
                firstNonBlank(tribunalTier, "TRIBUNAL"),
                firstNonBlank(materialityAxis, "MATERIALIDADE"),
                firstNonBlank(forumScope, "FORO"),
                firstNonBlank(concurrencyEnvelope, "CONCORRENCIA"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("justiceTrack", justiceTrack);
        out.put("tribunalTier", tribunalTier);
        out.put("riteFamily", riteFamily);
        out.put("materialityAxis", materialityAxis);
        out.put("forumScope", forumScope);
        out.put("territorialAnchor", territorialAnchor);
        out.put("admissibilityChannel", admissibilityChannel);
        out.put("executionTrack", executionTrack);
        out.put("recursalTrack", recursalTrack);
        out.put("preventionAnchor", preventionAnchor);
        out.put("concurrencyEnvelope", concurrencyEnvelope);
        out.put("descriptor", descriptor());
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
