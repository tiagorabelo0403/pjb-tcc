package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralJudicialPlacementFinalizerTest {

    @Test
    void mustApplyForumAllocationOverSeedAndBuildFinalForoLabel() {
        NationalProceduralForumLabelFactory forumLabelFactory = mock(NationalProceduralForumLabelFactory.class);
        NationalProceduralForumAllocationResolver forumAllocationResolver = mock(NationalProceduralForumAllocationResolver.class);
        NationalProceduralJudicialPlacementFinalizer finalizer = new NationalProceduralJudicialPlacementFinalizer(forumLabelFactory, forumAllocationResolver);
        NationalProceduralDistributionSuggestion distribution = new NationalProceduralDistributionSuggestion(
                Instant.now(),
                "VARA-01",
                "TJCE",
                "Fortaleza",
                "CE",
                "CIVEL",
                88.4d,
                true,
                "aderencia forte",
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
        NationalProceduralJudicialPlacementSeed seed = new NationalProceduralJudicialPlacementSeed(
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                "VARA-01",
                "CIVEL",
                "PJE",
                distribution
        );
        ProceduralForumAllocationReport forumAllocation = new ProceduralForumAllocationReport(
                Instant.now(),
                "7",
                "Procedimento Comum",
                "AUTOR",
                "Fortaleza",
                "CE",
                "domicilio do autor",
                "NENHUM",
                "NENHUM_SINAL",
                List.of(),
                "TJCE",
                "Tribunal de Justica do Ceara",
                "UJ-01",
                "1a Vara Civel de Fortaleza",
                "CIVEL",
                false,
                true,
                91.5d,
                "PJE",
                true,
                false,
                false,
                true,
                true,
                "READY_WITH_REVIEW",
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        when(forumAllocationResolver.resolve(any())).thenReturn(forumAllocation);
        when(forumLabelFactory.buildForoLabel("Fortaleza", "CE", TipoJustica.ESTADUAL)).thenReturn("Foro de Fortaleza/CE");

        NationalProceduralJudicialPlacement placement = finalizer.finalizePlacement(context(Map.of("cidadeAutor", "Fortaleza", "ufAutor", "CE")), seed);

        assertEquals("Fortaleza", placement.cidadeSugerida());
        assertEquals("CE", placement.ufSugerida());
        assertEquals("TJCE", placement.tribunalCodigo());
        assertEquals("Tribunal de Justica do Ceara", placement.tribunalNome());
        assertEquals("UJ-01", placement.varaSugerida());
        assertEquals("CIVEL", placement.tipoVaraSugerido());
        assertEquals("PJE", placement.judicialSystem());
        assertEquals("Foro de Fortaleza/CE", placement.foroSugerido());
        assertSame(distribution, placement.distribution());
        assertSame(forumAllocation, placement.forumAllocation());
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
