package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record NationalProceduralLinkageAnalysis(
        String preventionMode,
        String linkageMode,
        List<String> relatedProcessNumbers,
        List<String> reasons
) {

    NationalProceduralLinkageAnalysis {
        preventionMode = trimToNull(preventionMode);
        linkageMode = trimToNull(linkageMode);
        relatedProcessNumbers = PayloadMaps.copyDistinctStrings(relatedProcessNumbers);
        reasons = PayloadMaps.copyDistinctStrings(reasons);
    }

    Map<String, Object> metadata() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("preventionMode", preventionMode);
        out.put("linkageMode", linkageMode);
        out.put("relatedProcessNumbers", relatedProcessNumbers);
        out.put("reasons", reasons);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
