package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralAccelerationReport(
        Instant generatedAt,
        ProceduralAccelerationTrack track,
        ProceduralAccelerationLane lane,
        String accelerationProfile,
        int firstReviewTargetMinutes,
        int magistrateEscalationTargetMinutes,
        int technicalSupportTargetMinutes,
        boolean queueBypassEligible,
        boolean immediateHumanGate,
        boolean publicationLocked,
        boolean natJusPriorityRecommended,
        boolean protectiveUrgencyRecommended,
        boolean victimIdentityShieldRecommended,
        boolean multiChannelEscalation,
        boolean legalClockMonitoring,
        String recommendedDecisionBlueprint,
        String executivePrioritySummary,
        List<ProceduralAccelerationDirectiveItem> directives,
        List<String> evidenceChecklist,
        List<String> operationalChecklist,
        List<String> legalBases,
        List<String> alerts,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("track", track != null ? track.name() : null);
        out.put("lane", lane != null ? lane.name() : null);
        out.put("accelerationProfile", accelerationProfile);
        out.put("firstReviewTargetMinutes", firstReviewTargetMinutes);
        out.put("magistrateEscalationTargetMinutes", magistrateEscalationTargetMinutes);
        out.put("technicalSupportTargetMinutes", technicalSupportTargetMinutes);
        out.put("queueBypassEligible", queueBypassEligible);
        out.put("immediateHumanGate", immediateHumanGate);
        out.put("publicationLocked", publicationLocked);
        out.put("natJusPriorityRecommended", natJusPriorityRecommended);
        out.put("protectiveUrgencyRecommended", protectiveUrgencyRecommended);
        out.put("victimIdentityShieldRecommended", victimIdentityShieldRecommended);
        out.put("multiChannelEscalation", multiChannelEscalation);
        out.put("legalClockMonitoring", legalClockMonitoring);
        out.put("recommendedDecisionBlueprint", recommendedDecisionBlueprint);
        out.put("executivePrioritySummary", executivePrioritySummary);
        out.put("directives", directives == null ? List.of() : directives.stream().map(ProceduralAccelerationDirectiveItem::toMap).toList());
        out.put("evidenceChecklist", evidenceChecklist == null ? List.of() : evidenceChecklist);
        out.put("operationalChecklist", operationalChecklist == null ? List.of() : operationalChecklist);
        out.put("legalBases", legalBases == null ? List.of() : legalBases);
        out.put("alerts", alerts == null ? List.of() : alerts);
        LinkedHashMap<String, Object> safeMetadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safeMetadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        out.put("metadata", safeMetadata);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
