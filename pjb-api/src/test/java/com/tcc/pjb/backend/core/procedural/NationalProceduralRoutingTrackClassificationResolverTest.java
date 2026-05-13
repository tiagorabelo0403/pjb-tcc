package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralRoutingTrackClassificationResolverTest {

    @Test
    void mustResolveTrackClassificationSnapshotFromFoundation() {
        NationalProceduralComplexityBandResolver complexityBandResolver = Mockito.mock(NationalProceduralComplexityBandResolver.class);
        NationalProceduralTipoJusticaResolver tipoJusticaResolver = Mockito.mock(NationalProceduralTipoJusticaResolver.class);
        NationalProceduralClassificationResolver classificationResolver = Mockito.mock(NationalProceduralClassificationResolver.class);
        NationalProceduralRoutingTrackClassificationResolver resolver = new NationalProceduralRoutingTrackClassificationResolver(
                complexityBandResolver,
                tipoJusticaResolver,
                classificationResolver
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

        when(complexityBandResolver.resolve(any())).thenReturn("ALTA");
        when(tipoJusticaResolver.resolve(eq(sample.payload().get("tipoJustica")), eq(sample.competence()), eq(sample.canonical()), eq("COMUM_ORDINARIO"), eq(sample.partyProfile()))).thenReturn(TipoJustica.ESTADUAL);
        when(classificationResolver.resolveProceduralRegime(eq("COMUM_ORDINARIO"), eq(sample.actionProfile()), eq(sample.juizadoDecision()))).thenReturn("COMUM");
        when(classificationResolver.resolveProceduralTrack(eq("COMUM_ORDINARIO"), eq(sample.actionProfile()), eq(sample.juizadoDecision()), eq(TipoJustica.ESTADUAL))).thenReturn("FAZENDA_PUBLICA");

        NationalProceduralRoutingClassificationSnapshot snapshot = resolver.resolve(foundation);

        assertEquals("ALTA", snapshot.complexityBand());
        assertEquals("COMUM_ORDINARIO", snapshot.ritoSugerido());
        assertSame(TipoJustica.ESTADUAL, snapshot.tipoJustica());
        assertEquals("COMUM", snapshot.proceduralRegime());
        assertEquals("FAZENDA_PUBLICA", snapshot.proceduralTrack());
    }
}
