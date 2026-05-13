package com.tcc.pjb.backend.core.frontend.accessibility;

import java.util.List;
import java.util.Set;

public record PjbAccessToJusticeAssessment(String journeyCode,
                                           int score,
                                           String grade,
                                           Set<PjbAccessToJusticeMetric> coveredMetrics,
                                           List<String> gaps) {
}
