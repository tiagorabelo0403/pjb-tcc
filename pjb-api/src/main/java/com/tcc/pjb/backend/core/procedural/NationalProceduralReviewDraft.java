package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.List;

record NationalProceduralReviewDraft(
        List<String> reasons,
        List<String> legalBases,
        List<String> alerts,
        List<String> reviewChecklist,
        List<String> blockingIssues,
        List<String> actionMarkers
) {

    NationalProceduralReviewDraft {
        reasons = PayloadMaps.copyDistinctStrings(reasons);
        legalBases = PayloadMaps.copyDistinctStrings(legalBases);
        alerts = PayloadMaps.copyDistinctStrings(alerts);
        reviewChecklist = PayloadMaps.copyDistinctStrings(reviewChecklist);
        blockingIssues = PayloadMaps.copyDistinctStrings(blockingIssues);
        actionMarkers = PayloadMaps.copyDistinctStrings(actionMarkers);
    }
}
