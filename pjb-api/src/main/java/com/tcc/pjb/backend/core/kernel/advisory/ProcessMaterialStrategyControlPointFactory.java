package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.LinkedHashSet;
import java.util.List;

final class ProcessMaterialStrategyControlPointFactory {

    private final ProcessMaterialStrategyTextSupport textSupport;
    private final ProcessMaterialStrategyScoringPolicy scoringPolicy;

    ProcessMaterialStrategyControlPointFactory(ProcessMaterialStrategyTextSupport textSupport,
                                               ProcessMaterialStrategyScoringPolicy scoringPolicy) {
        this.textSupport = textSupport;
        this.scoringPolicy = scoringPolicy;
    }

    LinkedHashSet<String> create(ProcessMaterialStrategyInput input,
                                 List<String> signals,
                                 List<String> controversyAxes,
                                 List<String> thesisVectors,
                                 int gapCount,
                                 int evidenceScore,
                                 int negotiationScore,
                                 int readinessScore,
                                 int blockerCount) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(ProcessMaterialStrategyMessages.controlPointLitigationPosture(scoringPolicy.classifyLitigationPosture(evidenceScore, negotiationScore, gapCount, input.urgent())));
        out.add(ProcessMaterialStrategyMessages.controlPointProtocolReadiness(scoringPolicy.classifyProtocolReadiness(readinessScore, blockerCount)));
        out.add(ProcessMaterialStrategyMessages.controlPointNegotiationStance(scoringPolicy.classifyNegotiationStance(negotiationScore, gapCount, input.urgent())));
        out.add(ProcessMaterialStrategyMessages.controlPointEvidenceReadiness(scoringPolicy.classifyEvidenceReadiness(evidenceScore, gapCount)));
        if (!textSupport.blank(input.objectLabel())) {
            out.add(ProcessMaterialStrategyMessages.controlPointObject(textSupport.compact(input.objectLabel(), 190)));
        }
        if (!textSupport.blank(input.primaryRelief())) {
            out.add(ProcessMaterialStrategyMessages.controlPointPrimaryRelief(textSupport.compact(input.primaryRelief(), 190)));
        }
        signals.stream().limit(6).forEach(signal -> out.add(ProcessMaterialStrategyMessages.controlPointOperationalSignal(textSupport.compact(signal, 170))));
        if (controversyAxes.isEmpty()) {
            out.add(ProcessMaterialStrategyMessages.missingControversyControlPoint());
        }
        if (thesisVectors.isEmpty()) {
            out.add(ProcessMaterialStrategyMessages.missingThesisControlPoint());
        }
        return out;
    }
}
