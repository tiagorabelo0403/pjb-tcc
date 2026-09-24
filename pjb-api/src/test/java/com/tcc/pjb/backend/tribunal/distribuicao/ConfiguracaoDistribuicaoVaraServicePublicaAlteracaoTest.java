package com.tcc.pjb.backend.tribunal.distribuicao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.competencia.TipoVaraDistribuicao;
import com.tcc.pjb.backend.model.entity.competencia.Tribunal;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.competencia.UnidadesJudiciariasAlteradasEvent;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ConfiguracaoDistribuicaoVaraServicePublicaAlteracaoTest {

    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private ConfiguracaoDistribuicaoVaraService service;

    @BeforeEach
    void montar() {
        Tribunal tribunal = new Tribunal("TJCE", "Tribunal de Justica do Ceara", TipoJustica.ESTADUAL, GrauJurisdicao.SEGUNDO_GRAU, "CE");
        UnidadeJudiciariaCompetencia unidade = new UnidadeJudiciariaCompetencia(
                "VARA-01", tribunal, null, "CE", TipoJustica.ESTADUAL, RamoDireito.CIVIL, TipoVaraDistribuicao.CIVEL_GERAL);
        when(unidadeRepository.findByCodigo("VARA-01")).thenReturn(Optional.of(unidade));
        TribunalRuleEngine tribunalRuleEngine = mock(TribunalRuleEngine.class);
        when(tribunalRuleEngine.resolverLimiarCongestionamento(any(), any())).thenReturn(new BigDecimal("0.85"));
        service = new ConfiguracaoDistribuicaoVaraService(
                unidadeRepository,
                tribunalRuleEngine,
                mock(FederalismoJudicialEngine.class),
                mock(OutboxPublisher.class),
                new ObjectMapper(),
                eventPublisher);
    }

    @Test
    void bloquearDistribuicaoDaVaraAvisaQueAsUnidadesMudaram() {
        service.alterarStatusDistribuicao("VARA-01", false, ConfiguracaoDistribuicaoVaraService.MotivoRestricao.VARA_VAGA,
                "Vara vaga", null, "operador");

        verify(eventPublisher).publishEvent(any(UnidadesJudiciariasAlteradasEvent.class));
    }

    @Test
    void atualizarOcupacaoDaVaraAvisaQueAsUnidadesMudaram() {
        service.atualizarOcupacao("VARA-01", 10, "operador");

        verify(eventPublisher).publishEvent(any(UnidadesJudiciariasAlteradasEvent.class));
    }

    @Test
    void atualizarRestricaoOperacionalAvisaQueAsUnidadesMudaram() {
        service.atualizarRestricaoOperacional(new ConfiguracaoDistribuicaoVaraService.RestricaoOperacional(
                "VARA-01", false, ConfiguracaoDistribuicaoVaraService.MotivoRestricao.VARA_VAGA, "Vara vaga", null,
                false, false, null, null, null, true, null, Instant.now(), "operador"));

        verify(eventPublisher).publishEvent(any(UnidadesJudiciariasAlteradasEvent.class));
    }
}
