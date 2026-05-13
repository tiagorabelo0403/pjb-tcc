package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

record NationalProceduralTerritorialAnchor(
        String mode,
        String comarca,
        String uf,
        String reason
) {

    NationalProceduralTerritorialAnchor {
        mode = trimToNull(mode);
        comarca = trimToNull(comarca);
        uf = trimToNull(uf);
        reason = trimToNull(reason);
    }

    Map<String, Object> metadata() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", mode);
        out.put("comarca", comarca);
        out.put("uf", uf);
        out.put("reason", reason);
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
