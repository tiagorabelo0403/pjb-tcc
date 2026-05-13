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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfilePublicLawResolverTest {

    @Test
    void mustCloseTrabalhistaProfileWithSumarissimoSignalsAndGuardrails() {
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        when(tetoProcessualService.diagnosticar(any(BigDecimal.class), eq(TipoJustica.TRABALHO), eq(RamoDireito.TRABALHISTA), eq(RitoProcessual.TRABALHISTA_SUMARISSIMO), isNull(), any(LocalDate.class)))
                .thenReturn(TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("5000.00"), LocalDate.of(2026, 4, 4)));
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        when(salarioMinimoNacionalService.valorEm(any(LocalDate.class))).thenReturn(new BigDecimal("1621.00"));
        NationalProceduralActionProfileMessages messages = new NationalProceduralActionProfileMessages();
        NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver = new NationalProceduralActionProfileEconomicRitoResolver(tetoProcessualService, salarioMinimoNacionalService);
        NationalProceduralActionProfilePublicLawResolver resolver = new NationalProceduralActionProfilePublicLawResolver(
                new NationalProceduralActionProfileSpecialProcedureResolver(messages),
                new NationalProceduralActionProfileLaborCriminalResolver(economicRitoResolver, messages),
                new NationalProceduralActionProfilePublicEntityResolver(economicRitoResolver, messages),
                economicRitoResolver
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of("valorCausa", new BigDecimal("5000.00")),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "reclamacao trabalhista por verbas rescisorias e fgts",
                new NationalProceduralPartyProfile(false, false, false, false, true, false, false, false, false, List.of(), null, null)
        )).orElseThrow();

        assertEquals("RECLAMACAO_TRABALHISTA", profile.actionNature());
        assertEquals("TRABALHISTA_SUMARISSIMO", profile.defaultRito());
        assertTrue(profile.alerts().contains("Rito sumaríssimo trabalhista depende de liquidez, individualização do pedido e identificação precisa da parte reclamada."));
        assertTrue(profile.reviewChecklist().contains("Validar requisitos específicos do rito sumaríssimo trabalhista antes do protocolo."));
    }
}
