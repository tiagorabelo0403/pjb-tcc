package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record NationalProceduralActionProfile(
        String actionNature,
        String actionFamily,
        boolean specialProcedure,
        String defaultRito,
        String varaFamily,
        List<String> markers,
        List<String> reasons,
        List<String> legalBases,
        List<String> alerts,
        List<String> reviewChecklist
) {
    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("actionNature", actionNature);
        out.put("actionFamily", actionFamily);
        out.put("specialProcedure", specialProcedure);
        out.put("defaultRito", defaultRito);
        out.put("varaFamily", varaFamily);
        out.put("markers", markers);
        out.put("reasons", reasons);
        out.put("legalBases", legalBases);
        out.put("alerts", alerts);
        out.put("reviewChecklist", reviewChecklist);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
