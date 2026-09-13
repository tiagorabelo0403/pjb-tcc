package com.tcc.pjb.backend.core.frontend.accessibility;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class PjbAccessToJusticeScoreService {

    public PjbAccessToJusticeAssessment score(String journeyCode, Set<PjbAccessToJusticeMetric> covered) {
        EnumSet<PjbAccessToJusticeMetric> coveredMetrics = covered == null || covered.isEmpty()
                ? EnumSet.noneOf(PjbAccessToJusticeMetric.class)
                : EnumSet.copyOf(covered);
        LinkedHashSet<String> gaps = new LinkedHashSet<>();
        for (PjbAccessToJusticeMetric metric : PjbAccessToJusticeMetric.values()) {
            if (!coveredMetrics.contains(metric)) {
                gaps.add(metric.name());
            }
        }
        int score = Math.round((coveredMetrics.size() * 100.0f) / PjbAccessToJusticeMetric.values().length);
        String grade = score >= 90 ? "A" : score >= 75 ? "B" : score >= 60 ? "C" : "D";
        return new PjbAccessToJusticeAssessment(Objects.toString(journeyCode, "UNKNOWN"), score, grade, Set.copyOf(coveredMetrics), new ArrayList<>(gaps));
    }
}
