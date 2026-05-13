package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.List;

record NationalProceduralReviewInputAssessment(
        List<String> missingInputs,
        List<String> blockingIssues
) {

    NationalProceduralReviewInputAssessment {
        missingInputs = PayloadMaps.copyDistinctStrings(missingInputs);
        blockingIssues = PayloadMaps.copyDistinctStrings(blockingIssues);
    }
}
