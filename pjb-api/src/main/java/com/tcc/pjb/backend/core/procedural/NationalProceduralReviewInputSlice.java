package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.List;

record NationalProceduralReviewInputSlice(
        List<String> missingInputs,
        List<String> blockingIssues
) {

    NationalProceduralReviewInputSlice {
        missingInputs = PayloadMaps.copyDistinctStrings(missingInputs);
        blockingIssues = PayloadMaps.copyDistinctStrings(blockingIssues);
    }
}
