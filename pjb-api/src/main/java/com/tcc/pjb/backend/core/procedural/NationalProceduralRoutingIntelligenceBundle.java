package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record NationalProceduralRoutingIntelligenceBundle(
        ProceduralIntelligenceAdvisoryReport advisoryIntelligence,
        ProceduralDecisionQualityReport decisionQuality,
        ProceduralAutomationPolicyReport automationPolicy,
        ProceduralExecutiveExplainabilityReport executiveExplainability,
        ProceduralAccelerationReport acceleration
) {

    public NationalProceduralRoutingIntelligenceBundle {
        Objects.requireNonNull(advisoryIntelligence);
        Objects.requireNonNull(decisionQuality);
        Objects.requireNonNull(automationPolicy);
        Objects.requireNonNull(executiveExplainability);
        Objects.requireNonNull(acceleration);
    }

    public Map<String, Object> toMetadataEntries() {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("naturezaJuridicaCanonical", advisoryIntelligence.naturezaPrincipal() != null ? advisoryIntelligence.naturezaPrincipal().name() : null);
        metadata.put("advisoryIntelligence", advisoryIntelligence.toMap());
        metadata.put("decisionQuality", decisionQuality.toMap());
        metadata.put("operatingModeHint", decisionQuality.operatingModeHint());
        metadata.put("safeAutomationEligible", decisionQuality.safeAutomationEligible());
        metadata.put("automationPolicy", automationPolicy.toMap());
        metadata.put("automationMode", automationPolicy.mode() != null ? automationPolicy.mode().name() : null);
        metadata.put("automationDomain", automationPolicy.domain() != null ? automationPolicy.domain().name() : null);
        metadata.put("autoProtocolBlueprintEligible", automationPolicy.autoProtocolBlueprintEligible());
        metadata.put("autoRouteEligible", automationPolicy.autoRouteEligible());
        metadata.put("executiveExplainability", executiveExplainability.toMap());
        metadata.put("executiveSummary", executiveExplainability.summary());
        metadata.put("executiveActionFrame", executiveExplainability.actionFrame());
        metadata.put("acceleration", acceleration.toMap());
        metadata.put("accelerationTrack", acceleration.track() != null ? acceleration.track().name() : null);
        metadata.put("accelerationLane", acceleration.lane() != null ? acceleration.lane().name() : null);
        metadata.put("queueBypassEligible", acceleration.queueBypassEligible());
        metadata.put("immediateHumanGate", acceleration.immediateHumanGate());
        metadata.put("publicationLocked", acceleration.publicationLocked());
        metadata.put("priorityDecisionBlueprint", acceleration.recommendedDecisionBlueprint());
        metadata.put("prioritySummary", acceleration.executivePrioritySummary());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(metadata);
    }
}
