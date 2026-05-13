package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.tribunal.distribuicao.ConfiguracaoDistribuicaoVaraService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralForumAllocationSeedResolverTest {

    @Test
    void mustOrchestrateClassBaseAndProfileResolversAndPromotePerfilData() {
        NationalProceduralForumAllocationClassSeedResolver classSeedResolver = mock(NationalProceduralForumAllocationClassSeedResolver.class);
        NationalProceduralForumAllocationBaseSeedResolver baseSeedResolver = mock(NationalProceduralForumAllocationBaseSeedResolver.class);
        NationalProceduralForumAllocationProfileResolver profileResolver = mock(NationalProceduralForumAllocationProfileResolver.class);
        NationalProceduralForumAllocationSeedResolver resolver = new NationalProceduralForumAllocationSeedResolver(classSeedResolver, baseSeedResolver, profileResolver);
        NationalProceduralForumAllocationContext context = context();
        NationalProceduralForumAllocationClassSeed classSeed = new NationalProceduralForumAllocationClassSeed(RitoProcessual.COMUM_ORDINARIO, null);
        NationalProceduralForumAllocationBaseSeed baseSeed = new NationalProceduralForumAllocationBaseSeed(
                new NationalProceduralTerritorialAnchor("BASE_RESOLVIDA", "Fortaleza", "CE", "fundamento"),
                new NationalProceduralLinkageAnalysis("NENHUM", "NENHUM_SINAL", List.of(), List.of()),
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                null,
                "Vara base",
                "CIVEL",
                81.0d
        );
        ConfiguracaoDistribuicaoVaraService.PerfilVara perfil = mock(ConfiguracaoDistribuicaoVaraService.PerfilVara.class);
        when(perfil.varaId()).thenReturn("VARA-01");
        when(perfil.varaDescricao()).thenReturn("1a Vara Cível");
        when(perfil.tipoVara()).thenReturn(com.tcc.pjb.backend.model.entity.competencia.TipoVaraDistribuicao.CIVEL_GERAL);
        when(perfil.tribunalCodigo()).thenReturn("TJCE");
        when(perfil.comarcaId()).thenReturn("Fortaleza");
        when(perfil.uf()).thenReturn("CE");
        when(perfil.scoreDisponibilidade()).thenReturn(93.0d);

        when(classSeedResolver.resolve(context)).thenReturn(classSeed);
        when(baseSeedResolver.resolve(context)).thenReturn(baseSeed);
        when(profileResolver.resolve(context, classSeed, baseSeed)).thenReturn(perfil);

        NationalProceduralForumAllocationSeed result = resolver.resolve(context);

        assertSame(perfil, result.perfil());
        assertEquals("VARA-01", result.unidadeCodigo());
        assertEquals("1a Vara Cível", result.varaSugerida());
        assertEquals("CIVEL_GERAL", result.tipoVara());
        assertEquals(93.0d, result.distributionScore());
        verify(classSeedResolver).resolve(context);
        verify(baseSeedResolver).resolve(context);
        verify(profileResolver).resolve(context, classSeed, baseSeed);
    }

    private static NationalProceduralForumAllocationContext context() {
        return new NationalProceduralForumAllocationContext(
                Map.of("classe", "indenizacao", "materia", "CIVEL"),
                "obrigação de fazer",
                new ProceduralCanonicalResolver.CanonicalContext(
                        Instant.now(),
                        RitoProcessual.COMUM_ORDINARIO,
                        "CIVEL",
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
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.8d, List.of(), List.of(), Map.of()),
                TipoJustica.ESTADUAL,
                "COMUM_ORDINARIO",
                new NationalProceduralActionProfile("INDENIZATORIA", "CIVEL", false, "COMUM_ORDINARIO", "CIVEL", List.of(), List.of(), List.of(), List.of(), List.of()),
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.7d, false),
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                null,
                "CIVEL",
                null
        );
    }
}
