package com.tcc.pjb.backend.service.juiz.session;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GabineteSessionProfile(
        String sessionDesk,
        String sessionSecretariatDesk,
        String draftingDesk,
        String hearingSupportDesk,
        String hearingWindow,
        String sessionCadence,
        String colegiadoChannel,
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
        String escalationMode,
        boolean requiresClerkReinforcement,
        boolean requiresSessionReview,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public GabineteSessionProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(sessionDesk, "SESSAO"),
                firstNonBlank(draftingDesk, "MINUTA"),
                firstNonBlank(sessionCadence, "CADENCE"),
                firstNonBlank(escalationMode, "MODE"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("sessionDesk", sessionDesk);
        out.put("sessionSecretariatDesk", sessionSecretariatDesk);
        out.put("draftingDesk", draftingDesk);
        out.put("hearingSupportDesk", hearingSupportDesk);
        out.put("hearingWindow", hearingWindow);
        out.put("sessionCadence", sessionCadence);
        out.put("colegiadoChannel", colegiadoChannel);
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
        out.put("escalationMode", escalationMode);
        out.put("requiresClerkReinforcement", requiresClerkReinforcement);
        out.put("requiresSessionReview", requiresSessionReview);
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
