package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingClassificationAnalyzer {

    private final NationalProceduralRoutingTrackClassificationResolver trackClassificationResolver;
    private final NationalProceduralRoutingPlacementReviewResolver placementReviewResolver;

    public NationalProceduralRoutingClassificationAnalyzer(NationalProceduralRoutingTrackClassificationResolver trackClassificationResolver,
                                                           NationalProceduralRoutingPlacementReviewResolver placementReviewResolver) {
        this.trackClassificationResolver = Objects.requireNonNull(trackClassificationResolver);
        this.placementReviewResolver = Objects.requireNonNull(placementReviewResolver);
    }

    NationalProceduralRoutingCoreResolution analyze(NationalProceduralRoutingFoundationResolution foundation) {
        Objects.requireNonNull(foundation);
        NationalProceduralRoutingClassificationSnapshot classification = trackClassificationResolver.resolve(foundation);
        NationalProceduralRoutingPlacementReviewResolution placementReview = placementReviewResolver.resolve(foundation, classification);
        return new NationalProceduralRoutingCoreResolution(
                foundation.payload(),
                foundation.corpus(),
                foundation.sourceLabel(),
                foundation.partyProfile(),
                foundation.selectedRito(),
                foundation.canonical(),
                foundation.competence(),
                foundation.actionProfile(),
                foundation.probatoryProfile(),
                foundation.teto(),
                foundation.juizadoDecision(),
                classification.complexityBand(),
                classification.ritoSugerido(),
                classification.tipoJustica(),
                classification.proceduralRegime(),
                classification.proceduralTrack(),
                placementReview.judicialPlacement(),
                placementReview.reviewSynthesis()
        );
    }
}
