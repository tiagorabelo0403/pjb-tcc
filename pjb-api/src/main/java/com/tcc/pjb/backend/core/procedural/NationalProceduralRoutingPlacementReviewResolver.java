package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingPlacementReviewResolver {

    private final NationalProceduralJudicialPlacementResolver judicialPlacementResolver;
    private final NationalProceduralReviewSynthesisResolver reviewSynthesisResolver;

    public NationalProceduralRoutingPlacementReviewResolver(NationalProceduralJudicialPlacementResolver judicialPlacementResolver,
                                                            NationalProceduralReviewSynthesisResolver reviewSynthesisResolver) {
        this.judicialPlacementResolver = Objects.requireNonNull(judicialPlacementResolver);
        this.reviewSynthesisResolver = Objects.requireNonNull(reviewSynthesisResolver);
    }

    NationalProceduralRoutingPlacementReviewResolution resolve(NationalProceduralRoutingFoundationResolution foundation,
                                                               NationalProceduralRoutingClassificationSnapshot classification) {
        Objects.requireNonNull(foundation);
        Objects.requireNonNull(classification);
        NationalProceduralJudicialPlacement judicialPlacement = judicialPlacementResolver.resolve(
                new NationalProceduralJudicialPlacementContext(
                        foundation.payload(),
                        foundation.corpus(),
                        foundation.canonical(),
                        foundation.competence(),
                        classification.tipoJustica(),
                        classification.ritoSugerido(),
                        classification.proceduralTrack(),
                        foundation.actionProfile(),
                        foundation.juizadoDecision()
                )
        );
        NationalProceduralReviewSynthesis reviewSynthesis = reviewSynthesisResolver.resolve(new NationalProceduralReviewSynthesisContext(
                        foundation.payload(),
                        foundation.selectedRito(),
                        foundation.competence(),
                        foundation.actionProfile(),
                        foundation.juizadoDecision(),
                        foundation.partyProfile(),
                        foundation.teto(),
                        judicialPlacement.forumAllocation(),
                        judicialPlacement.distribution(),
                        classification.tipoJustica(),
                        judicialPlacement.cidadeSugerida(),
                        judicialPlacement.ufSugerida()
                )
        );
        return new NationalProceduralRoutingPlacementReviewResolution(judicialPlacement, reviewSynthesis);
    }
}
