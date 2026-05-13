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

class NationalProceduralJudicialPlacementSeedResolverTest {

    @Test
    void mustBuildSeedFromDistributionAndFallbackData() {
        NationalProceduralDistributionResolver distributionResolver = mock(NationalProceduralDistributionResolver.class);
        NationalProceduralForumLabelFactory forumLabelFactory = mock(NationalProceduralForumLabelFactory.class);
        NationalProceduralJudicialPlacementSeedResolver resolver = new NationalProceduralJudicialPlacementSeedResolver(distributionResolver, forumLabelFactory);
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
                List.of("alerta operacional"),
                List.of("validar rito"),
                List.of(),
                Map.of("origem", "mapa")
        );

        when(distributionResolver.resolve(any())).thenReturn(java.util.Optional.of(distribution));
        when(forumLabelFactory.buildVaraLabel(any(), any(), any(), any(), any())).thenReturn("Vara fallback");

        NationalProceduralJudicialPlacementSeed seed = resolver.resolve(context(Map.of("cidadeAutor", "Fortaleza", "ufAutor", "CE")));

        assertEquals("Fortaleza", seed.cidadeSugerida());
        assertEquals("CE", seed.ufSugerida());
        assertEquals("TJCE", seed.tribunalCodigo());
        assertEquals("TJCE", seed.tribunalNome());
        assertEquals("VARA-01", seed.varaSugerida());
        assertEquals("CIVEL", seed.tipoVaraSugerido());
        assertEquals("PJE", seed.judicialSystem());
        assertSame(distribution, seed.distribution());
    }

    @Test
    void mustKeepFallbackValuesWhenDistributionIsMissing() {
        NationalProceduralDistributionResolver distributionResolver = mock(NationalProceduralDistributionResolver.class);
        NationalProceduralForumLabelFactory forumLabelFactory = mock(NationalProceduralForumLabelFactory.class);
        NationalProceduralJudicialPlacementSeedResolver resolver = new NationalProceduralJudicialPlacementSeedResolver(distributionResolver, forumLabelFactory);

        when(distributionResolver.resolve(any())).thenReturn(java.util.Optional.empty());
        when(forumLabelFactory.buildVaraLabel(any(), any(), any(), any(), any())).thenReturn("Vara Civel");

        NationalProceduralJudicialPlacementSeed seed = resolver.resolve(context(Map.of("cidadeAutor", "Quixada", "ufAutor", "CE", "tribunalCodigo", "TJCE")));

        assertEquals("Quixada", seed.cidadeSugerida());
        assertEquals("CE", seed.ufSugerida());
        assertEquals("TJCE", seed.tribunalCodigo());
        assertEquals("TJCE", seed.tribunalNome());
        assertEquals("Vara Civel", seed.varaSugerida());
        assertEquals("CIVEL", seed.tipoVaraSugerido());
        assertEquals("PJE", seed.judicialSystem());
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
