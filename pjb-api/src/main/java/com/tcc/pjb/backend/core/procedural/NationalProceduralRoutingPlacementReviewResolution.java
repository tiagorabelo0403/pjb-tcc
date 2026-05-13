package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;

record NationalProceduralRoutingPlacementReviewResolution(
        NationalProceduralJudicialPlacement judicialPlacement,
        NationalProceduralReviewSynthesis reviewSynthesis
) {

    NationalProceduralRoutingPlacementReviewResolution {
        Objects.requireNonNull(judicialPlacement);
        Objects.requireNonNull(reviewSynthesis);
    }
}
