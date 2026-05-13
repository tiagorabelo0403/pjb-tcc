package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfilePublicEntityResolverTest {

    @Test
    void mustClosePrevidenciarioBpcWithDedicatedSubtype() {
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        when(salarioMinimoNacionalService.valorEm(any(LocalDate.class))).thenReturn(new BigDecimal("1621.00"));
        when(tetoProcessualService.diagnosticar(any(BigDecimal.class), eq(TipoJustica.FEDERAL), eq(RamoDireito.PREVIDENCIARIO), eq(RitoProcessual.PREVIDENCIARIO_JEF), isNull(), any(LocalDate.class)))
                .thenReturn(TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("12000.00"), LocalDate.of(2026, 4, 4)));
        NationalProceduralActionProfilePublicEntityResolver resolver = new NationalProceduralActionProfilePublicEntityResolver(
                new NationalProceduralActionProfileEconomicRitoResolver(tetoProcessualService, salarioMinimoNacionalService),
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of("valorCausa", new BigDecimal("12000.00")),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "pedido de bpc loas perante o inss por miserabilidade social",
                null
        )).orElseThrow();

        assertEquals("PREVIDENCIARIO_BPC_LOAS", profile.defaultRito());
        assertEquals("BPC_LOAS", profile.actionNature());
        assertTrue(profile.reviewChecklist().contains("Conferir deficiência ou idade, miserabilidade e documentação socioassistencial mínima do BPC/LOAS."));
    }

    @Test
    void mustClosePrevidenciarioRppsWithDedicatedSubtype() {
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        NationalProceduralActionProfilePublicEntityResolver resolver = new NationalProceduralActionProfilePublicEntityResolver(
                new NationalProceduralActionProfileEconomicRitoResolver(tetoProcessualService, salarioMinimoNacionalService),
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "pedido previdenciario de regime proprio de previdencia rpps por servidor publico estadual",
                null
        )).orElseThrow();

        assertEquals("PREVIDENCIARIO_RPPS", profile.defaultRito());
        assertEquals("PREVIDENCIARIO_RPPS", profile.actionNature());
        assertTrue(profile.reviewChecklist().contains("Conferir regime próprio aplicável, vínculo estatutário e ato administrativo previdenciário correlato."));
    }


    @Test
    void mustCloseAdministrativoPadWithDedicatedTrack() {
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        NationalProceduralActionProfilePublicEntityResolver resolver = new NationalProceduralActionProfilePublicEntityResolver(
                new NationalProceduralActionProfileEconomicRitoResolver(tetoProcessualService, salarioMinimoNacionalService),
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "processo administrativo disciplinar com comissao processante e penalidade disciplinar a servidor publico estadual",
                null
        )).orElseThrow();

        assertEquals("ADMINISTRATIVO_PAD", profile.defaultRito());
        assertEquals("PROCESSO_ADMINISTRATIVO_DISCIPLINAR", profile.actionNature());
        assertTrue(profile.alerts().contains("PAD exige controle de competência instauradora, comissão processante, contraditório, ampla defesa e prescrição disciplinar."));
    }



    @Test
    void catalogMustExposeAdministrativePadDocuments() {
        var snapshot = ProceduralCatalogSupport.snapshot(com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.ADMINISTRATIVO_PAD);
        assertTrue(snapshot.documents().stream().anyMatch(doc -> doc.code().equals("PORTARIA_INSTAURACAO") && doc.required()));
        assertTrue(snapshot.documents().stream().anyMatch(doc -> doc.code().equals("DESIGNACAO_COMISSAO") && doc.required()));
    }

}
