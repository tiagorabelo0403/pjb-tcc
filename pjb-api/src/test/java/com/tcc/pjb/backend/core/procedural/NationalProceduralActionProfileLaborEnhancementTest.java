package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfileLaborEnhancementTest {

    @Test
    void mustClassifyLaborAlcadaWhenValueFitsTwoMinimumWages() {
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        when(salarioMinimoNacionalService.valorEm(any(LocalDate.class))).thenReturn(new BigDecimal("1621.00"));
        when(tetoProcessualService.diagnosticar(any(BigDecimal.class), eq(TipoJustica.TRABALHO), eq(RamoDireito.TRABALHISTA), eq(RitoProcessual.TRABALHISTA_SUMARISSIMO), isNull(), any(LocalDate.class)))
                .thenReturn(TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("2500.00"), LocalDate.of(2026, 4, 6)));
        NationalProceduralActionProfileMessages messages = new NationalProceduralActionProfileMessages();
        NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver = new NationalProceduralActionProfileEconomicRitoResolver(tetoProcessualService, salarioMinimoNacionalService);
        NationalProceduralActionProfileLaborCriminalResolver resolver = new NationalProceduralActionProfileLaborCriminalResolver(economicRitoResolver, messages);

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of("valorCausa", new BigDecimal("2500.00")),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "reclamacao trabalhista de alcada por verbas rescisorias",
                new NationalProceduralPartyProfile(false, false, false, false, false, true, false, false, false, List.of(), null, null)
        )).orElseThrow();

        assertEquals("TRABALHISTA_SUMARIO_ALCADA", profile.defaultRito());
        assertTrue(profile.alerts().contains("Rito de alçada trabalhista exige controle do valor da causa, revisão em 48 horas e restrição recursal, salvo matéria constitucional."));
    }

    @Test
    void mustClassifyLaborInquiryForSeriousMisconduct() {
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        when(salarioMinimoNacionalService.valorEm(any(LocalDate.class))).thenReturn(new BigDecimal("1621.00"));
        NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver = new NationalProceduralActionProfileEconomicRitoResolver(tetoProcessualService, salarioMinimoNacionalService);
        NationalProceduralActionProfileMessages messages = new NationalProceduralActionProfileMessages();
        NationalProceduralActionProfileLaborCriminalResolver resolver = new NationalProceduralActionProfileLaborCriminalResolver(economicRitoResolver, messages);

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "inquerito judicial para apuracao de falta grave ajuizado apos suspensao do empregado estavel",
                new NationalProceduralPartyProfile(false, false, false, false, false, true, false, false, false, List.of(), null, null)
        )).orElseThrow();

        assertEquals("TRABALHISTA_INQUERITO_FALTA_GRAVE", profile.defaultRito());
        assertTrue(profile.reviewChecklist().contains("Validar suspensão do empregado, estabilidade invocada e prazo decadencial contado da suspensão."));
    }

    @Test
    void mustClassifyMilitaryIpmWithDedicatedTrack() {
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver = new NationalProceduralActionProfileEconomicRitoResolver(tetoProcessualService, salarioMinimoNacionalService);
        NationalProceduralActionProfileMessages messages = new NationalProceduralActionProfileMessages();
        NationalProceduralActionProfileLaborCriminalResolver resolver = new NationalProceduralActionProfileLaborCriminalResolver(economicRitoResolver, messages);

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "inquerito policial militar instaurado em auditoria militar com encarregado do ipm",
                new NationalProceduralPartyProfile(false, false, false, false, false, false, false, true, false, List.of(), null, null)
        )).orElseThrow();

        assertEquals("MILITAR_IPM", profile.defaultRito());
        assertTrue(profile.reviewChecklist().contains("Verificar portaria de instauração, autoridade encarregada, materialidade militar e cadeia de custódia mínima do IPM."));
    }

    @Test
    void catalogMustExposeLaborSpecialProceduresWithStructuredDocumentsAndStages() {
        var alcada = ProceduralCatalogSupport.snapshot(RitoProcessual.TRABALHISTA_SUMARIO_ALCADA);
        var inquerito = ProceduralCatalogSupport.snapshot(RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE);
        var cumprimento = ProceduralCatalogSupport.snapshot(RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO);

        assertTrue(alcada.documents().stream().anyMatch(doc -> doc.code().equals("MEMORIA_VALOR_CAUSA") && doc.required()));
        assertTrue(inquerito.documents().stream().anyMatch(doc -> doc.code().equals("CONTROLE_PRAZO_DECADENCIAL") && doc.required()));
        assertTrue(cumprimento.documents().stream().anyMatch(doc -> doc.code().equals("INSTRUMENTO_COLETIVO") && doc.required()));
        assertTrue(cumprimento.stages().stream().anyMatch(stage -> stage.getFase().equals("CUMPRIMENTO_SENTENCA")));

        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        assertEquals(TpuClasseCnj.RECLAMACAO_TRABALHISTA_INDIVIDUAL, catalogService.resolveClasseTpu(null, RitoProcessual.TRABALHISTA_SUMARIO_ALCADA).orElseThrow());
        assertEquals(TpuClasseCnj.RECLAMACAO_TRABALHISTA_INDIVIDUAL, catalogService.resolveClasseTpu(null, RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE).orElseThrow());
        assertEquals(TpuClasseCnj.CUMPRIMENTO_ACORDO_TRABALHISTA, catalogService.resolveClasseTpu(null, RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO).orElseThrow());
    }
}
