package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ProceduralAccelerationDirectiveItem(
        ProceduralAccelerationDirectiveCode code,
        String message,
        boolean blocking,
        String source,
        String detail
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code != null ? code.name() : null);
        out.put("message", message);
        out.put("blocking", blocking);
        out.put("source", source);
        out.put("detail", detail);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
