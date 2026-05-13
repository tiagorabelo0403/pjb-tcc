package com.tcc.pjb.backend.judicial.connectors.domain;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorStructureNode(
        String moduleKey,
        JudicialConnectorStructuralArea area,
        String targetPackage,
        List<String> legacyPackages,
        List<String> coreComponents,
        List<String> responsibilities,
        Map<String, Object> metadata
) {
    public JudicialConnectorStructureNode {
        legacyPackages = legacyPackages == null ? List.of() : List.copyOf(legacyPackages);
        coreComponents = coreComponents == null ? List.of() : List.copyOf(coreComponents);
        responsibilities = responsibilities == null ? List.of() : List.copyOf(responsibilities);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("moduleKey", moduleKey);
        out.put("area", area != null ? area.name() : null);
        out.put("targetPackage", targetPackage);
        out.put("legacyPackages", legacyPackages);
        out.put("coreComponents", coreComponents);
        out.put("responsibilities", responsibilities);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
