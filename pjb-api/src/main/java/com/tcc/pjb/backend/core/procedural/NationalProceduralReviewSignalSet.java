package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.List;

record NationalProceduralReviewSignalSet(
        List<String> alerts,
        List<String> reviewChecklist,
        List<String> blockingIssues
) {

    NationalProceduralReviewSignalSet {
        alerts = PayloadMaps.copyDistinctStrings(alerts);
        reviewChecklist = PayloadMaps.copyDistinctStrings(reviewChecklist);
        blockingIssues = PayloadMaps.copyDistinctStrings(blockingIssues);
    }
}
