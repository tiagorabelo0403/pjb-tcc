package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralActionProfileResolverOrchestrationTest {

    @Test
    void mustPreferPublicLawResolutionBeforePrivateFallback() {
        NationalProceduralActionProfilePublicLawResolver publicLawResolver = Mockito.mock(NationalProceduralActionProfilePublicLawResolver.class);
        NationalProceduralActionProfilePrivateRightsResolver privateRightsResolver = Mockito.mock(NationalProceduralActionProfilePrivateRightsResolver.class);
        NationalProceduralActionProfileResolver resolver = new NationalProceduralActionProfileResolver(publicLawResolver, privateRightsResolver);
        NationalProceduralActionProfile profile = new NationalProceduralActionProfile("MANDADO_SEGURANCA", "CONSTITUCIONAL", true, "ESPECIAL_MANDADO_SEGURANCA", "MANDADO_SEGURANCA", List.of(), List.of(), List.of(), List.of(), List.of());

        when(publicLawResolver.resolve(any())).thenReturn(Optional.of(profile));

        NationalProceduralActionProfile result = resolver.resolve(Map.of(), NationalProceduralRoutingTestFixtures.sampleResolution().canonical(), "mandado de seguranca", null);

        assertSame(profile, result);
        verify(publicLawResolver).resolve(any());
        verify(privateRightsResolver, never()).resolve(any());
    }
}
