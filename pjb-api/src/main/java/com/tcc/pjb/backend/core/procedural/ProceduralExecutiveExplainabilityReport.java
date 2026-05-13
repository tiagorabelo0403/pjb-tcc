package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralExecutiveExplainabilityReport(
        String summary,
        String actionFrame,
        List<ProceduralExecutiveExplanationItem> items,
        List<String> highlights,
        List<String> legalAnchors,
        Map<String, Object> metadata
) {
    public ProceduralExecutiveExplainabilityReport {
        items = items == null ? List.of() : List.copyOf(items);
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        legalAnchors = legalAnchors == null ? List.of() : List.copyOf(legalAnchors);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("summary", summary);
        out.put("actionFrame", actionFrame);
        out.put("items", items.stream().map(ProceduralExecutiveExplanationItem::toMap).toList());
        out.put("highlights", highlights);
        out.put("legalAnchors", legalAnchors);
        out.put("metadata", metadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
