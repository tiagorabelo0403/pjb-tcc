package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationSessionDoctorSnapshot(
        String status,
        boolean blockedSurface,
        boolean driftDetected,
        String operationalMode,
        List<String> blockedSkillIds,
        List<String> blockedToolExampleIds,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("blockedSurface", blockedSurface);
        out.put("driftDetected", driftDetected);
        out.put("operationalMode", operationalMode);
        out.put("blockedSkillIds", blockedSkillIds == null ? List.of() : List.copyOf(blockedSkillIds));
        out.put("blockedToolExampleIds", blockedToolExampleIds == null ? List.of() : List.copyOf(blockedToolExampleIds));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
