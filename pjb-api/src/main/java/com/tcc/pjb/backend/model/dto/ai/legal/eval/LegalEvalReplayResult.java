package com.tcc.pjb.backend.model.dto.ai.legal.eval;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalEvalReplayResult(
        String replayId,
        String suiteId,
        String scope,
        boolean passed,
        double qualityScore,
        List<LegalEvalMetric> metrics,
        List<String> promotionCandidates,
        List<String> demotionCandidates,
        Map<String, Object> adaptationHints,
        LegalEvalReplayArtifact artifact
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("replayId", replayId);
        out.put("suiteId", suiteId);
        out.put("scope", scope);
        out.put("passed", passed);
        out.put("qualityScore", qualityScore);
        out.put("metrics", metrics == null ? List.of() : metrics.stream().map(LegalEvalMetric::asMap).toList());
        out.put("promotionCandidates", promotionCandidates == null ? List.of() : List.copyOf(promotionCandidates));
        out.put("demotionCandidates", demotionCandidates == null ? List.of() : List.copyOf(demotionCandidates));
        out.put("adaptationHints", adaptationHints == null ? Map.of() : Map.copyOf(adaptationHints));
        out.put("artifact", artifact == null ? Map.of() : artifact.asMap());
        return Collections.unmodifiableMap(out);
    }
}
