package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.List;

record NationalProceduralReviewSynthesis(
        List<String> reasons,
        List<String> legalBases,
        List<String> alerts,
        List<String> missingInputs,
        List<String> actionMarkers,
        List<String> reviewChecklist,
        List<String> blockingIssues,
        double confidence,
        boolean requiresHumanReview,
        String riskLevel
) {

    NationalProceduralReviewSynthesis {
        reasons = PayloadMaps.copyDistinctStrings(reasons);
        legalBases = PayloadMaps.copyDistinctStrings(legalBases);
        alerts = PayloadMaps.copyDistinctStrings(alerts);
        missingInputs = PayloadMaps.copyDistinctStrings(missingInputs);
        actionMarkers = PayloadMaps.copyDistinctStrings(actionMarkers);
        reviewChecklist = PayloadMaps.copyDistinctStrings(reviewChecklist);
        blockingIssues = PayloadMaps.copyDistinctStrings(blockingIssues);
        riskLevel = riskLevel == null ? null : riskLevel.trim();
    }
}
