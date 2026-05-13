package com.tcc.pjb.backend.core.processo.juizado.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record NationalProceduralJuizadoDecision(
        boolean admiteJuizado,
        String ritoOverride,
        List<String> reasons,
        List<String> legalBases,
        List<String> alerts,
        List<String> reviewChecklist,
        double confidence,
        boolean requiresReview
) {
    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("admiteJuizado", admiteJuizado);
        out.put("ritoOverride", ritoOverride);
        out.put("reasons", reasons);
        out.put("legalBases", legalBases);
        out.put("alerts", alerts);
        out.put("reviewChecklist", reviewChecklist);
        out.put("confidence", confidence);
        out.put("requiresReview", requiresReview);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
