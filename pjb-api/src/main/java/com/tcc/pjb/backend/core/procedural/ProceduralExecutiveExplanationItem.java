package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ProceduralExecutiveExplanationItem(
        ProceduralExecutiveExplanationCode code,
        String severity,
        boolean actionRequired,
        String message,
        String detail
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code != null ? code.name() : null);
        out.put("severity", severity);
        out.put("actionRequired", actionRequired);
        out.put("message", message);
        out.put("detail", detail);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
