package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

final class ProcessMaterialStrategyMetricsFactory {

    Map<String, Object> create(ProcessMaterialStrategyInput input,
                               int evidenceScore,
                               int negotiationScore,
                               int readinessScore,
                               int gapCount,
                               int anchorCount,
                               int checklistCount,
                               int signalCount,
                               int controversyCount,
                               int thesisCount) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("lane", input.lane());
        metrics.put("evidenceScore", evidenceScore);
        metrics.put("negotiationScore", negotiationScore);
        metrics.put("readinessScore", readinessScore);
        metrics.put("proofGapCount", gapCount);
        metrics.put("evidenceAnchorCount", anchorCount);
        metrics.put("controversyAxisCount", controversyCount);
        metrics.put("thesisVectorCount", thesisCount);
        metrics.put("protocolChecklistCount", checklistCount);
        metrics.put("signalCount", signalCount);
        metrics.put("urgent", input.urgent());
        metrics.put("juizado", input.juizado());
        metrics.put("valorCausa", input.valorCausa() != null ? input.valorCausa().setScale(2, RoundingMode.HALF_UP) : null);
        metrics.put("ramoDireito", input.ramoDireito());
        metrics.put("rito", input.ritoName());
        return metrics;
    }
}
