package com.tcc.pjb.backend.core.processual.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;

class NationalProcessRoutingServiceTest {

    @Test
    void shouldRouteCearaCivilCaseToStateCourt() {
        TribunalRuleEngine ruleEngine = Mockito.mock(TribunalRuleEngine.class);
        when(ruleEngine.resolverPrazoDias(any(), any(), anyInt())).thenReturn(48);
        when(ruleEngine.resolverBooleano(any(), any(), anyBoolean())).thenReturn(true);
        when(ruleEngine.resolverLimiteJuizadoEmReais(any())).thenReturn(new BigDecimal("60720.00"));
        NationalProcessRoutingService service = new NationalProcessRoutingService(
                ruleEngine,
                Mockito.mock(TerritorialRoutingResolver.class),
                Mockito.mock(RelationalRoutingResolver.class),
                Mockito.mock(FracionaryOrganRoutingResolver.class),
                Mockito.mock(ProceduralCoverageResolver.class)
        );
        var result = service.route(new NationalProcessRoutingService.RoutingCommand(
                RitoProcessual.COMUM_ORDINARIO,
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "CE",
                "Quixadá",
                new BigDecimal("10000.00"),
                "Ação de cobrança",
                "Cobrança contratual",
                null
        ));
        assertEquals(TipoJustica.ESTADUAL, result.tipoJustica());
        assertEquals("TJCE", result.tribunalCodigo());
        assertTrue(result.conciliacaoObrigatoria());
        assertNotNull(result.unidadeJudiciariaCodigo());
        assertFalse(result.alertas().isEmpty());
    }


    @Test
    void shouldSurfaceUrgencyAndChecklistSignalsForLinkedPayload() {
        TribunalRuleEngine ruleEngine = Mockito.mock(TribunalRuleEngine.class);
        when(ruleEngine.resolverPrazoDias(any(), any(), anyInt())).thenReturn(24);
        when(ruleEngine.resolverBooleano(any(), any(), anyBoolean())).thenReturn(false);
        when(ruleEngine.resolverLimiteJuizadoEmReais(any())).thenReturn(new BigDecimal("60720.00"));
        NationalProcessRoutingService service = new NationalProcessRoutingService(
                ruleEngine,
                Mockito.mock(TerritorialRoutingResolver.class),
                Mockito.mock(RelationalRoutingResolver.class),
                Mockito.mock(FracionaryOrganRoutingResolver.class),
                Mockito.mock(ProceduralCoverageResolver.class)
        );
        var result = service.route(new NationalProcessRoutingService.RoutingCommand(
                RitoProcessual.CIVIL_TUTELA_URGENTE,
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "CE",
                "Fortaleza",
                new BigDecimal("5000.00"),
                "Tutela de urgência",
                "Obrigação de fazer",
                null,
                null,
                "Fortaleza",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "0000100-00.2026.8.06.0001",
                null,
                false,
                true,
                false,
                true,
                false,
                true,
                false
        ));
        assertEquals("PLANTAO_PRIORITARIO", result.distributionMode());
        assertEquals("MODERADO", result.routingRiskLevel());
        assertTrue(result.alertas().stream().anyMatch(item -> item.contains("Pedido liminar declarado")));
        assertTrue(result.reviewChecklist().stream().anyMatch(item -> item.contains("Perfil sugerido de mesa/unidade")));
    }

}
