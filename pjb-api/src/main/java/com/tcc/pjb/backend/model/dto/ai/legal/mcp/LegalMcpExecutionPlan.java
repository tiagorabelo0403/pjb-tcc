package com.tcc.pjb.backend.model.dto.ai.legal.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayResult;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalMcpExecutionPlan(
        String planId,
        String selectionMode,
        boolean discoveryEnabled,
        String transportProfile,
        String authorizationProfile,
        String batchingStrategy,
        String completionStrategy,
        String trustMode,
        int evidenceBudget,
        int serverBudget,
        List<LegalMcpServerDescriptor> pinnedServers,
        List<LegalMcpServerDescriptor> fallbackServers,
        List<String> pinnedToolIds,
        List<LegalMcpSkillDescriptor> pinnedSkills,
        List<LegalMcpToolExample> pinnedToolExamples,
        LegalMcpDeliberationPlan deliberation,
        LegalMcpContextCompactionPlan contextCompaction,
        LegalMcpExecutionTranscript transcript,
        LegalMcpDoctorReport doctor,
        LegalMcpEvidencePromotionDecision evidencePromotion,
        List<String> reasons,
        List<String> safeguards,
        LegalEvalReplayResult evaluation
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("planId", planId);
        out.put("selectionMode", selectionMode);
        out.put("discoveryEnabled", discoveryEnabled);
        out.put("transportProfile", transportProfile);
        out.put("authorizationProfile", authorizationProfile);
        out.put("batchingStrategy", batchingStrategy);
        out.put("completionStrategy", completionStrategy);
        out.put("trustMode", trustMode);
        out.put("evidenceBudget", evidenceBudget);
        out.put("serverBudget", serverBudget);
        out.put("pinnedServers", pinnedServers == null ? List.of() : pinnedServers.stream().map(LegalMcpServerDescriptor::asMap).toList());
        out.put("fallbackServers", fallbackServers == null ? List.of() : fallbackServers.stream().map(LegalMcpServerDescriptor::asMap).toList());
        out.put("pinnedToolIds", pinnedToolIds == null ? List.of() : List.copyOf(pinnedToolIds));
        out.put("pinnedSkills", pinnedSkills == null ? List.of() : pinnedSkills.stream().map(LegalMcpSkillDescriptor::asMap).toList());
        out.put("pinnedToolExamples", pinnedToolExamples == null ? List.of() : pinnedToolExamples.stream().map(LegalMcpToolExample::asMap).toList());
        out.put("deliberation", deliberation == null ? Map.of() : deliberation.asMap());
        out.put("contextCompaction", contextCompaction == null ? Map.of() : contextCompaction.asMap());
        out.put("transcript", transcript == null ? Map.of() : transcript.asMap());
        out.put("doctor", doctor == null ? Map.of() : doctor.asMap());
        out.put("evidencePromotion", evidencePromotion == null ? Map.of() : evidencePromotion.asMap());
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        out.put("safeguards", safeguards == null ? List.of() : List.copyOf(safeguards));
        out.put("evaluation", evaluation == null ? Map.of() : evaluation.asMap());
        return Collections.unmodifiableMap(out);
    }
}
