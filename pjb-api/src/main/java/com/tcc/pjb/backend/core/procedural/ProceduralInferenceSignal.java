package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public sealed interface ProceduralInferenceSignal permits ProceduralInferenceSignal.TextualSignal,
        ProceduralInferenceSignal.StructuredSignal,
        ProceduralInferenceSignal.ConflictSignal,
        ProceduralInferenceSignal.ConfidenceSignal {

    String code();

    String origin();

    double weight();

    boolean critical();

    String summary();

    default Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("type", type());
        out.put("code", code());
        out.put("origin", origin());
        out.put("weight", weight());
        out.put("critical", critical());
        out.put("summary", summary());
        extraToMap(out);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    private String type() {
        return switch (this) {
            case TextualSignal ignored -> "TEXT";
            case StructuredSignal ignored -> "STRUCTURED";
            case ConflictSignal ignored -> "CONFLICT";
            case ConfidenceSignal ignored -> "CONFIDENCE";
        };
    }

    private void extraToMap(Map<String, Object> out) {
        switch (this) {
            case TextualSignal signal -> {
                out.put("snippet", signal.snippet());
                out.put("normalized", signal.normalized());
            }
            case StructuredSignal signal -> {
                out.put("field", signal.field());
                out.put("value", signal.value());
            }
            case ConflictSignal signal -> {
                out.put("axis", signal.axis());
                out.put("leftValue", signal.leftValue());
                out.put("rightValue", signal.rightValue());
            }
            case ConfidenceSignal signal -> {
                out.put("confidence", signal.confidence());
                out.put("band", signal.band());
            }
        }
    }

    record TextualSignal(
            String code,
            String origin,
            double weight,
            boolean critical,
            String summary,
            String snippet,
            String normalized
    ) implements ProceduralInferenceSignal {
    }

    record StructuredSignal(
            String code,
            String origin,
            double weight,
            boolean critical,
            String summary,
            String field,
            String value
    ) implements ProceduralInferenceSignal {
    }

    record ConflictSignal(
            String code,
            String origin,
            double weight,
            boolean critical,
            String summary,
            String axis,
            String leftValue,
            String rightValue
    ) implements ProceduralInferenceSignal {
    }

    record ConfidenceSignal(
            String code,
            String origin,
            double weight,
            boolean critical,
            String summary,
            double confidence,
            String band
    ) implements ProceduralInferenceSignal {
    }
}
