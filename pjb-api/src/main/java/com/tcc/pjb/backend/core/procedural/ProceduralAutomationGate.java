package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public sealed interface ProceduralAutomationGate permits ProceduralAutomationGate.HardGate, ProceduralAutomationGate.SoftGate {

    String code();

    String source();

    String description();

    String detail();

    boolean blocking();

    default Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("kind", this instanceof HardGate ? "HARD" : "SOFT");
        out.put("code", code());
        out.put("source", source());
        out.put("description", description());
        out.put("detail", detail());
        out.put("blocking", blocking());
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    record HardGate(String code, String source, String description, String detail) implements ProceduralAutomationGate {
        @Override
        public boolean blocking() {
            return true;
        }
    }

    record SoftGate(String code, String source, String description, String detail) implements ProceduralAutomationGate {
        @Override
        public boolean blocking() {
            return false;
        }
    }
}
