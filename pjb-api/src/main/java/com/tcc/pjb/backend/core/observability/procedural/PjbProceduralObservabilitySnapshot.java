package com.tcc.pjb.backend.core.observability.procedural;

import java.util.List;
import java.util.Map;

public record PjbProceduralObservabilitySnapshot(String status,
                                                int criticalSignals,
                                                Map<PjbProceduralObservationType, Long> signalsByType,
                                                List<String> operationalActions) {
}
