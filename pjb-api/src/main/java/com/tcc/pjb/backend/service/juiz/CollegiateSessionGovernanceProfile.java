package com.tcc.pjb.backend.service.juiz;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CollegiateSessionGovernanceProfile(
        String chamberLabel,
        String relatoriaDesk,
        String publicationDesk,
        String sessionRoom,
        String quorumLabel,
        String publicationMode,
        String deliberationMode,
        String reviewerDesk,
        String divergenceDesk,
        String voteAuditDesk,
        String proclamationDesk,
        String judgmentSequence,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public CollegiateSessionGovernanceProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(chamberLabel, "ORGAO"),
                firstNonBlank(relatoriaDesk, "RELATORIA"),
                firstNonBlank(sessionRoom, "SALA"),
                firstNonBlank(publicationMode, "PUBLICACAO"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("chamberLabel", chamberLabel);
        out.put("relatoriaDesk", relatoriaDesk);
        out.put("publicationDesk", publicationDesk);
        out.put("sessionRoom", sessionRoom);
        out.put("quorumLabel", quorumLabel);
        out.put("publicationMode", publicationMode);
        out.put("deliberationMode", deliberationMode);
        out.put("reviewerDesk", reviewerDesk);
        out.put("divergenceDesk", divergenceDesk);
        out.put("voteAuditDesk", voteAuditDesk);
        out.put("proclamationDesk", proclamationDesk);
        out.put("judgmentSequence", judgmentSequence);
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
