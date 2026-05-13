package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralActionProfilePrivateRightsResolverTest {

    @Test
    void mustOrchestratePrivateRightsLanesBeforeFallingBackToConsumerResolver() {
        NationalProceduralActionProfileInfanciaResolver infanciaResolver = Mockito.mock(NationalProceduralActionProfileInfanciaResolver.class);
        NationalProceduralActionProfileFamilyResolver familyResolver = Mockito.mock(NationalProceduralActionProfileFamilyResolver.class);
        NationalProceduralActionProfilePropertyResolver propertyResolver = Mockito.mock(NationalProceduralActionProfilePropertyResolver.class);
        NationalProceduralActionProfileBusinessResolver businessResolver = Mockito.mock(NationalProceduralActionProfileBusinessResolver.class);
        NationalProceduralActionProfileConsumerResolver consumerResolver = Mockito.mock(NationalProceduralActionProfileConsumerResolver.class);
        NationalProceduralActionProfilePrivateRightsResolver resolver = new NationalProceduralActionProfilePrivateRightsResolver(
                infanciaResolver,
                familyResolver,
                propertyResolver,
                businessResolver,
                consumerResolver
        );

        NationalProceduralActionProfileContext context = new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "pedido de inventario",
                null
        );
        NationalProceduralActionProfile familyProfile = new NationalProceduralActionProfile(
                "INVENTARIO_ARROLAMENTO",
                "CIVIL_SUCESSOES",
                true,
                "CIVIL_INVENTARIO_ARROLAMENTO",
                "SUCESSOES",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of()
        );

        when(infanciaResolver.resolve(context)).thenReturn(Optional.empty());
        when(familyResolver.resolve(context)).thenReturn(Optional.of(familyProfile));

        NationalProceduralActionProfile result = resolver.resolve(context);

        assertSame(familyProfile, result);
        verify(infanciaResolver).resolve(context);
        verify(familyResolver).resolve(context);
        verify(propertyResolver, never()).resolve(context);
        verify(businessResolver, never()).resolve(context);
        verify(consumerResolver, never()).resolve(context);
    }
}
