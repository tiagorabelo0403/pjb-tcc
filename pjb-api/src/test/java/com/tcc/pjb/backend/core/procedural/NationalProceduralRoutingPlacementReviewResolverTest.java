package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralRoutingPlacementReviewResolverTest {

    @Test
    void mustResolvePlacementAndReviewFromFoundationAndClassification() {
        NationalProceduralJudicialPlacementResolver judicialPlacementResolver = Mockito.mock(NationalProceduralJudicialPlacementResolver.class);
        NationalProceduralReviewSynthesisResolver reviewSynthesisResolver = Mockito.mock(NationalProceduralReviewSynthesisResolver.class);
        NationalProceduralRoutingPlacementReviewResolver resolver = new NationalProceduralRoutingPlacementReviewResolver(
                judicialPlacementResolver,
                reviewSynthesisResolver
        );

        NationalProceduralRoutingCoreResolution sample = NationalProceduralRoutingTestFixtures.sampleResolution();
        NationalProceduralRoutingFoundationResolution foundation = new NationalProceduralRoutingFoundationResolution(
                sample.payload(),
                sample.corpus(),
                sample.sourceLabel(),
                sample.partyProfile(),
                sample.selectedRito(),
                sample.canonical(),
                sample.competence(),
                sample.actionProfile(),
                sample.probatoryProfile(),
                sample.teto(),
                sample.juizadoDecision()
        );
        NationalProceduralRoutingClassificationSnapshot classification = new NationalProceduralRoutingClassificationSnapshot(
                "ALTA",
                "COMUM_ORDINARIO",
                sample.tipoJustica(),
                "COMUM",
                "FAZENDA_PUBLICA"
        );
        NationalProceduralReviewSynthesis reviewSynthesis = new NationalProceduralReviewSynthesis(
                List.of("razao"),
                List.of("base"),
                List.of("alerta"),
                List.of(),
                List.of("MARCADOR"),
                List.of("check"),
                List.of(),
                0.86d,
                true,
                "MEDIUM"
        );

        when(judicialPlacementResolver.resolve(any())).thenReturn(sample.judicialPlacement());
        when(reviewSynthesisResolver.resolve(any())).thenReturn(reviewSynthesis);

        NationalProceduralRoutingPlacementReviewResolution result = resolver.resolve(foundation, classification);

        assertSame(sample.judicialPlacement(), result.judicialPlacement());
        assertSame(reviewSynthesis, result.reviewSynthesis());
        verify(judicialPlacementResolver).resolve(any());
        verify(reviewSynthesisResolver).resolve(any());
    }
}
