package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationSessionBootstrapSnapshot(
        String status,
        boolean blockedCapability,
        boolean repeatedDriftDetected,
        String operationalMode,
        String profileGate,
        String sigiloFence,
        List<String> mandatorySkillIds,
        List<String> mandatoryToolExampleIds,
        List<String> missingSkillIds,
        List<String> missingToolExampleIds,
        List<String> reasons,
        Map<String, Object> diagnostics
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("blockedCapability", blockedCapability);
        out.put("repeatedDriftDetected", repeatedDriftDetected);
        out.put("operationalMode", operationalMode);
        out.put("profileGate", profileGate);
        out.put("sigiloFence", sigiloFence);
        out.put("mandatorySkillIds", mandatorySkillIds == null ? List.of() : List.copyOf(mandatorySkillIds));
        out.put("mandatoryToolExampleIds", mandatoryToolExampleIds == null ? List.of() : List.copyOf(mandatoryToolExampleIds));
        out.put("missingSkillIds", missingSkillIds == null ? List.of() : List.copyOf(missingSkillIds));
        out.put("missingToolExampleIds", missingToolExampleIds == null ? List.of() : List.copyOf(missingToolExampleIds));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("diagnostics", diagnostics == null ? Map.of() : Map.copyOf(diagnostics));
        return Collections.unmodifiableMap(out);
    }
}
