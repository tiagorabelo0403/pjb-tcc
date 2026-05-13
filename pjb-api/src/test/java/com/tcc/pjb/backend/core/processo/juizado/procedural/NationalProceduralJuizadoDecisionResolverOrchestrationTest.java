package com.tcc.pjb.backend.core.processo.juizado.procedural;

import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingTestFixtures;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralJuizadoDecisionResolverOrchestrationTest {

    @Test
    void mustPreferExclusionGateBeforeTrackResolution() {
        NationalProceduralJuizadoExclusionResolver exclusionResolver = Mockito.mock(NationalProceduralJuizadoExclusionResolver.class);
        NationalProceduralJuizadoTrackResolver trackResolver = Mockito.mock(NationalProceduralJuizadoTrackResolver.class);
        NationalProceduralJuizadoDecisionResolver resolver = new NationalProceduralJuizadoDecisionResolver(exclusionResolver, trackResolver);
        NationalProceduralJuizadoDecision decision = new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of("alerta"), List.of(), 0.9d, true);

        when(exclusionResolver.resolve(any())).thenReturn(Optional.of(decision));

        NationalProceduralJuizadoDecision result = resolver.resolve(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().competence(),
                NationalProceduralRoutingTestFixtures.sampleResolution().actionProfile(),
                NationalProceduralRoutingTestFixtures.sampleResolution().partyProfile(),
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("1000.00"), LocalDate.of(2026, 4, 4)),
                "corpus"
        );

        assertSame(decision, result);
        verify(exclusionResolver).resolve(any());
        verify(trackResolver, never()).resolve(any());
    }
}
