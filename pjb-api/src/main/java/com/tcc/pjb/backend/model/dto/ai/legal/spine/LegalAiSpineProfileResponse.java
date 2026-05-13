package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiSpineProfileResponse(
        String profileCode,
        String version,
        String capability,
        Map<String, Object> policyVariables,
        List<LegalAiStructuredOutputDescriptor> structuredOutputs,
        List<LegalAiToolDescriptor> routedTools,
        LegalAiRetrievalDescriptor retrieval,
        LegalAiMemoryScopeDescriptor memory,
        LegalAiValidationDescriptor validation,
        LegalAiGraphDescriptor graph,
        LegalAiMultimodalDescriptor multimodal,
        LegalAiEvaluationDescriptor evaluation,
        LegalAiHallucinationGuardDescriptor hallucinationGuard,
        LegalAiTraceDescriptor trace,
        LegalAiApprovalDescriptor approval
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("profileCode", profileCode);
        out.put("version", version);
        out.put("capability", capability);
        out.put("policyVariables", policyVariables == null ? Map.of() : Map.copyOf(policyVariables));
        out.put("structuredOutputs", structuredOutputs == null ? List.of() : structuredOutputs.stream().map(LegalAiStructuredOutputDescriptor::asMap).toList());
        out.put("routedTools", routedTools == null ? List.of() : routedTools.stream().map(tool -> Map.of(
                "id", tool.id(),
                "label", tool.label(),
                "category", tool.category(),
                "readOnly", tool.readOnly(),
                "mcpEnabled", tool.mcpEnabled(),
                "ragAware", tool.ragAware(),
                "requiresStepUp", tool.requiresStepUp(),
                "sourceLane", tool.sourceLane()
        )).toList());
        out.put("retrieval", retrieval == null ? Map.of() : retrieval.asMap());
        out.put("memory", memory == null ? Map.of() : memory.asMap());
        out.put("validation", validation == null ? Map.of() : validation.asMap());
        out.put("graph", graph == null ? Map.of() : graph.asMap());
        out.put("multimodal", multimodal == null ? Map.of() : multimodal.asMap());
        out.put("evaluation", evaluation == null ? Map.of() : evaluation.asMap());
        out.put("hallucinationGuard", hallucinationGuard == null ? Map.of() : hallucinationGuard.asMap());
        out.put("trace", trace == null ? Map.of() : trace.asMap());
        out.put("approval", approval == null ? Map.of() : approval.asMap());
        return Collections.unmodifiableMap(out);
    }
}
