package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LegalAiPreConsciousFrameSnapshot(
        String status,
        String mode,
        String authorityFloor,
        String cognitivePosture,
        int riskScore,
        boolean responseAllowed,
        boolean humanReviewRequired,
        boolean learningCandidate,
        List<LegalAiJuridicalLineageDescriptor> lineages,
        List<LegalAiPreConsciousSignal> signals,
        List<String> metadataKeys,
        List<String> dominantLenses,
        List<String> authorityChecks,
        List<String> nextActions,
        Map<String, Object> metadata
) {
    public LegalAiPreConsciousFrameSnapshot {
        status = safe(status);
        mode = safe(mode);
        authorityFloor = safe(authorityFloor);
        cognitivePosture = safe(cognitivePosture);
        riskScore = Math.max(0, Math.min(100, riskScore));
        lineages = safeLineages(lineages);
        signals = safeSignals(signals);
        metadataKeys = safeList(metadataKeys);
        dominantLenses = safeList(dominantLenses);
        authorityChecks = safeList(authorityChecks);
        nextActions = safeList(nextActions);
        metadata = ImmutableViewSupport.map(metadata == null ? Map.of() : metadata);
    }

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("mode", mode);
        out.put("authorityFloor", authorityFloor);
        out.put("cognitivePosture", cognitivePosture);
        out.put("riskScore", riskScore);
        out.put("responseAllowed", responseAllowed);
        out.put("humanReviewRequired", humanReviewRequired);
        out.put("learningCandidate", learningCandidate);
        out.put("lineages", lineages.stream().map(LegalAiJuridicalLineageDescriptor::asMap).toList());
        out.put("signals", signals.stream().map(LegalAiPreConsciousSignal::asMap).toList());
        out.put("metadataKeys", metadataKeys);
        out.put("dominantLenses", dominantLenses);
        out.put("authorityChecks", authorityChecks);
        out.put("nextActions", nextActions);
        out.put("metadata", metadata);
        return Collections.unmodifiableMap(out);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> safeList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
    }

    private static List<LegalAiJuridicalLineageDescriptor> safeLineages(List<LegalAiJuridicalLineageDescriptor> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream().filter(Objects::nonNull).toList();
    }

    private static List<LegalAiPreConsciousSignal> safeSignals(List<LegalAiPreConsciousSignal> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream().filter(Objects::nonNull).toList();
    }
}
