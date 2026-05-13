package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralAutomationPolicyReport(
        Instant generatedAt,
        ProceduralAutomationDomain domain,
        ProceduralAutomationMode mode,
        boolean autoClassifyEligible,
        boolean autoRouteEligible,
        boolean autoProtocolBlueprintEligible,
        boolean autoSigiloSuggestionEligible,
        boolean autoDispatchHintEligible,
        List<ProceduralAutomationCapability> allowedCapabilities,
        List<ProceduralAutomationCapability> blockedCapabilities,
        List<ProceduralAutomationGate> gates,
        List<String> rationale,
        List<String> riskFactors,
        Map<String, Object> metadata
) {

    public ProceduralAutomationPolicyReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        allowedCapabilities = allowedCapabilities == null ? List.of() : List.copyOf(allowedCapabilities);
        blockedCapabilities = blockedCapabilities == null ? List.of() : List.copyOf(blockedCapabilities);
        gates = gates == null ? List.of() : List.copyOf(gates);
        rationale = rationale == null ? List.of() : List.copyOf(rationale);
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("domain", domain != null ? domain.name() : null);
        out.put("domainLabel", domain != null ? domain.label() : null);
        out.put("mode", mode != null ? mode.name() : null);
        out.put("autoClassifyEligible", autoClassifyEligible);
        out.put("autoRouteEligible", autoRouteEligible);
        out.put("autoProtocolBlueprintEligible", autoProtocolBlueprintEligible);
        out.put("autoSigiloSuggestionEligible", autoSigiloSuggestionEligible);
        out.put("autoDispatchHintEligible", autoDispatchHintEligible);
        out.put("allowedCapabilities", allowedCapabilities.stream().map(Enum::name).toList());
        out.put("blockedCapabilities", blockedCapabilities.stream().map(Enum::name).toList());
        out.put("gates", gates.stream().map(ProceduralAutomationGate::toMap).toList());
        out.put("rationale", rationale);
        out.put("riskFactors", riskFactors);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
