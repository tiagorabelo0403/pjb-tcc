package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;

public record LegalMcpDeliberationPlan(
        boolean required,
        String mode,
        String checkpointId,
        List<String> reasons,
        List<String> requiredSkillIds,
        List<String> requiredExampleIds
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("required", required);
        out.put("mode", mode);
        out.put("checkpointId", checkpointId);
        out.put("reasons", clean(reasons));
        out.put("requiredSkillIds", clean(requiredSkillIds));
        out.put("requiredExampleIds", clean(requiredExampleIds));
        return Collections.unmodifiableMap(out);
    }

    private static List<String> clean(List<String> source) {
        return source == null ? List.of() : source.stream().filter(Objects::nonNull).toList();
    }
}
