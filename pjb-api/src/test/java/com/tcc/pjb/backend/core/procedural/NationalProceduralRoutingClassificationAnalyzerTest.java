package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralRoutingClassificationAnalyzerTest {

    @Test
    void mustOrchestrateTrackClassificationAndPlacementReviewFromFoundationSnapshot() {
        NationalProceduralRoutingTrackClassificationResolver trackClassificationResolver = Mockito.mock(NationalProceduralRoutingTrackClassificationResolver.class);
        NationalProceduralRoutingPlacementReviewResolver placementReviewResolver = Mockito.mock(NationalProceduralRoutingPlacementReviewResolver.class);
        NationalProceduralRoutingClassificationAnalyzer analyzer = new NationalProceduralRoutingClassificationAnalyzer(
                trackClassificationResolver,
                placementReviewResolver
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
                TipoJustica.ESTADUAL,
                "COMUM",
                "FAZENDA_PUBLICA"
        );
        NationalProceduralRoutingPlacementReviewResolution placementReview = new NationalProceduralRoutingPlacementReviewResolution(
                sample.judicialPlacement(),
                sample.reviewSynthesis()
        );

        when(trackClassificationResolver.resolve(foundation)).thenReturn(classification);
        when(placementReviewResolver.resolve(foundation, classification)).thenReturn(placementReview);

        NationalProceduralRoutingCoreResolution result = analyzer.analyze(foundation);

        assertEquals("ALTA", result.complexityBand());
        assertEquals("COMUM_ORDINARIO", result.ritoSugerido());
        assertSame(TipoJustica.ESTADUAL, result.tipoJustica());
        assertEquals("COMUM", result.proceduralRegime());
        assertEquals("FAZENDA_PUBLICA", result.proceduralTrack());
        assertSame(sample.judicialPlacement(), result.judicialPlacement());
        assertSame(sample.reviewSynthesis(), result.reviewSynthesis());
        verify(trackClassificationResolver).resolve(foundation);
        verify(placementReviewResolver).resolve(foundation, classification);
    }
}
