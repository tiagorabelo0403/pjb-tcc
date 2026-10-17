package com.tcc.pjb.backend.tribunal.distribuicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.competencia.StatusOperacionalUnidadeJudiciaria;
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
import org.springframework.test.util.ReflectionTestUtils;

class ConfiguracaoDistribuicaoVaraServicePublicaAlteracaoTest {

    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private ConfiguracaoDistribuicaoVaraService service;
    private UnidadeJudiciariaCompetencia unidade;

    @BeforeEach
    void montar() {
        Tribunal tribunal = new Tribunal("TJCE", "Tribunal de Justica do Ceara", TipoJustica.ESTADUAL, GrauJurisdicao.SEGUNDO_GRAU, "CE");
        unidade = new UnidadeJudiciariaCompetencia(
                "VARA-01", tribunal, null, "CE", TipoJustica.ESTADUAL, RamoDireito.CIVIL, TipoVaraDistribuicao.CIVEL_GERAL);
        unidade.setCapacidadeMaxima(100);
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
        boolean alterada = service.alterarStatusDistribuicao("VARA-01", false,
                ConfiguracaoDistribuicaoVaraService.MotivoRestricao.VARA_VAGA, "Vara vaga", null, "operador");

        assertThat(alterada).isTrue();
        assertThat(unidade.isAceitaDistribuicao()).isFalse();
        assertThat(unidade.getStatusOperacional()).isEqualTo(StatusOperacionalUnidadeJudiciaria.BLOQUEADA);
        verify(unidadeRepository).save(unidade);
        verify(eventPublisher).publishEvent(any(UnidadesJudiciariasAlteradasEvent.class));
    }

    @Test
    void atualizarOcupacaoDaVaraAvisaQueAsUnidadesMudaram() {
        service.atualizarOcupacao("VARA-01", 10, "operador");

        assertThat(unidade.getProcessosAtivos()).isEqualTo(10);
        assertThat(unidade.getStatusOperacional()).isEqualTo(StatusOperacionalUnidadeJudiciaria.ATIVA);
        verify(eventPublisher).publishEvent(any(UnidadesJudiciariasAlteradasEvent.class));
    }

    @Test
    void congestionamentoAltoNaoSuspendeDistribuicaoDaVaraCompetente() {
        unidade.setAceitaDistribuicao(true);

        service.atualizarOcupacao("VARA-01", 95, "operador");

        assertThat(unidade.isAceitaDistribuicao()).isTrue();
        assertThat(unidade.getStatusOperacional()).isEqualTo(StatusOperacionalUnidadeJudiciaria.ATIVA);
        ConfiguracaoDistribuicaoVaraService.ResultadoConsulta consulta = service.consultar("VARA-01",
                new ConfiguracaoDistribuicaoVaraService.FiltroDistribuicao(
                        null, null, null, null, null, false, true, "CE", null, false, false, false));
        assertThat(consulta.aceita()).isTrue();
    }

    private static ConfiguracaoDistribuicaoVaraService.FiltroDistribuicao filtro(boolean requerJuizado) {
        return new ConfiguracaoDistribuicaoVaraService.FiltroDistribuicao(
                null, null, null, null, null, requerJuizado, true, "CE", null, false, false, false);
    }

    @Test
    void varaComJuizadoAdjuntoRecebeRitoComumEJuizado() {
        unidade.setPermiteJuizadoEspecial(true);

        assertThat(service.consultar("VARA-01", filtro(false)).aceita()).isTrue();
        assertThat(service.consultar("VARA-01", filtro(true)).aceita()).isTrue();
    }

    @Test
    void juizadoEspecialExclusivoNaoRecebeProcessoEmQueOAutorEscolheuORitoComum() {
        unidade.setExclusivoJuizadoEspecial(true);

        assertThat(service.consultar("VARA-01", filtro(false)).aceita()).isFalse();
        assertThat(service.consultar("VARA-01", filtro(false)).motivoRecusa()).isEqualTo("Juizado especial exclusivo nao recebe causa de rito comum");
        assertThat(service.consultar("VARA-01", filtro(true)).aceita()).isTrue();
    }

    @Test
    void juizadoDeRamoConsumidorAceitaCausaCivelDeJuizadoMasNaoCausaCivelComum() {
        ReflectionTestUtils.setField(unidade, "ramoDireito", RamoDireito.CONSUMIDOR);
        unidade.setPermiteJuizadoEspecial(true);

        assertThat(service.consultar("VARA-01", filtroComMateria("CIVIL", true)).aceita()).isTrue();
        assertThat(service.consultar("VARA-01", filtroComMateria("CIVIL", false)).aceita()).isFalse();
    }

    @Test
    void juizadoCriminalNaoRecebeCausaCivelDeJuizado() {
        unidade.setPermiteJuizadoEspecial(true);
        ReflectionTestUtils.setField(unidade, "ramoDireito", RamoDireito.PENAL);

        assertThat(service.consultar("VARA-01", filtroComMateria("CIVIL", true)).aceita()).isFalse();
    }

    @Test
    void juizadoCivelNaoRecebeCausaCriminalDeJuizado() {
        unidade.setPermiteJuizadoEspecial(true);
        ReflectionTestUtils.setField(unidade, "ramoDireito", RamoDireito.CONSUMIDOR);

        assertThat(service.consultar("VARA-01", filtroComMateria("PENAL", true)).aceita()).isFalse();
    }

    private static ConfiguracaoDistribuicaoVaraService.FiltroDistribuicao filtroComMateria(String materia, boolean requerJuizado) {
        return new ConfiguracaoDistribuicaoVaraService.FiltroDistribuicao(
                materia, null, null, null, null, requerJuizado, true, "CE", null, false, false, false);
    }

    @Test
    void varaSemJuizadoRecusaCausaDeJuizadoComMotivoProprio() {
        assertThat(service.consultar("VARA-01", filtro(true)).motivoRecusa()).isEqualTo("Vara nao processa causas de juizado especial");
    }

    @Test
    void sugestaoDeRedistribuicaoDeVaraAdjuntaSoApontaVaraQueTambemProcessaJuizado() {
        Tribunal tribunal = new Tribunal("TJCE", "Tribunal de Justica do Ceara", TipoJustica.ESTADUAL, GrauJurisdicao.SEGUNDO_GRAU, "CE");
        unidade.setPermiteJuizadoEspecial(true);
        unidade.setProcessosAtivos(95);
        UnidadeJudiciariaCompetencia semJuizado = new UnidadeJudiciariaCompetencia(
                "VARA-02", tribunal, null, "CE", TipoJustica.ESTADUAL, RamoDireito.CIVIL, TipoVaraDistribuicao.CIVEL_GERAL);
        semJuizado.setCapacidadeMaxima(100);
        semJuizado.setProcessosAtivos(10);
        UnidadeJudiciariaCompetencia outraAdjunta = new UnidadeJudiciariaCompetencia(
                "VARA-03", tribunal, null, "CE", TipoJustica.ESTADUAL, RamoDireito.CIVIL, TipoVaraDistribuicao.CIVEL_GERAL);
        outraAdjunta.setCapacidadeMaxima(100);
        outraAdjunta.setProcessosAtivos(20);
        outraAdjunta.setPermiteJuizadoEspecial(true);
        for (UnidadeJudiciariaCompetencia item : java.util.List.of(unidade, semJuizado, outraAdjunta)) {
            item.setComarca("Fortaleza");
        }
        when(unidadeRepository.findAll()).thenReturn(java.util.List.of(unidade, semJuizado, outraAdjunta));
        service.recarregarDoRepositorio();
        String comarcaId = service.buscarPerfil("VARA-01").orElseThrow().comarcaId();

        assertThat(service.sugerirRedistribuicao(comarcaId, "TJCE"))
                .extracting(ConfiguracaoDistribuicaoVaraService.SugestaoRedistribuicao::varaOrigem,
                        ConfiguracaoDistribuicaoVaraService.SugestaoRedistribuicao::varaDestino)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("VARA-01", "VARA-03"));
    }

    @Test
    void atualizarRestricaoOperacionalAvisaQueAsUnidadesMudaram() {
        service.atualizarRestricaoOperacional(new ConfiguracaoDistribuicaoVaraService.RestricaoOperacional(
                "VARA-01", false, ConfiguracaoDistribuicaoVaraService.MotivoRestricao.VARA_VAGA, "Vara vaga", null,
                false, false, null, null, null, true, null, Instant.now(), "operador"));

        assertThat(unidade.isAceitaDistribuicao()).isFalse();
        assertThat(unidade.getStatusOperacional()).isEqualTo(StatusOperacionalUnidadeJudiciaria.BLOQUEADA);
        verify(eventPublisher).publishEvent(any(UnidadesJudiciariasAlteradasEvent.class));
    }
}
