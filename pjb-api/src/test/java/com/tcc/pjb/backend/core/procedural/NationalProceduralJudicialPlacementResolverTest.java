package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralJudicialPlacementResolverTest {

    @Test
    void mustOrchestrateSeedResolutionAndFinalPlacement() {
        NationalProceduralJudicialPlacementSeedResolver seedResolver = mock(NationalProceduralJudicialPlacementSeedResolver.class);
        NationalProceduralJudicialPlacementFinalizer finalizer = mock(NationalProceduralJudicialPlacementFinalizer.class);
        NationalProceduralJudicialPlacementResolver resolver = new NationalProceduralJudicialPlacementResolver(seedResolver, finalizer);
        NationalProceduralJudicialPlacementContext context = context(Map.of("cidadeAutor", "Fortaleza", "ufAutor", "CE"));
        NationalProceduralJudicialPlacementSeed seed = new NationalProceduralJudicialPlacementSeed(
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                "VARA-01",
                "CIVEL",
                "PJE",
                null
        );
        NationalProceduralJudicialPlacement placement = new NationalProceduralJudicialPlacement(
                "Foro de Fortaleza/CE",
                "Fortaleza",
                "CE",
                "TJCE",
                "Tribunal de Justica do Ceara",
                "UJ-01",
                "CIVEL",
                "PJE",
                null,
                null
        );

        when(seedResolver.resolve(context)).thenReturn(seed);
        when(finalizer.finalizePlacement(context, seed)).thenReturn(placement);

        NationalProceduralJudicialPlacement result = resolver.resolve(context);

        assertSame(placement, result);
        verify(seedResolver).resolve(context);
        verify(finalizer).finalizePlacement(context, seed);
    }

    private static NationalProceduralJudicialPlacementContext context(Map<String, Object> payload) {
        return new NationalProceduralJudicialPlacementContext(
                payload,
                "obrigacao de fazer civel",
                new ProceduralCanonicalResolver.CanonicalContext(
                        Instant.now(),
                        RitoProcessual.COMUM_ORDINARIO,
                        "CIVIL",
                        "7",
                        "Procedimento Comum",
                        "ESTADUAL",
                        "TJCE",
                        "TJCE",
                        "PJE",
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.87d, List.of(), List.of(), Map.of()),
                TipoJustica.ESTADUAL,
                "COMUM_ORDINARIO",
                "CIVEL",
                new NationalProceduralActionProfile("INDENIZATORIA", "CIVEL", false, "COMUM_ORDINARIO", "CIVEL", List.of(), List.of(), List.of(), List.of(), List.of()),
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.84d, false)
        );
    }
}
