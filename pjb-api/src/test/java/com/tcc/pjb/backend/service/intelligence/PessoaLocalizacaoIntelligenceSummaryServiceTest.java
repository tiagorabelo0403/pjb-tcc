package com.tcc.pjb.backend.service.intelligence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoConsultaGovernada;
import com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoCanalConsulta;
import com.tcc.pjb.backend.model.repository.intelligence.PessoaLocalizacaoConsultaGovernadaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

class PessoaLocalizacaoIntelligenceSummaryServiceTest {

    @Test
    void deveUsarContagensPersistidasSemTruncarHistoricoRecente() {
        PessoaLocalizacaoConsultaGovernadaRepository repository = Mockito.mock(PessoaLocalizacaoConsultaGovernadaRepository.class);
        PjbTimeService timeService = new PjbTimeService(
                Clock.fixed(Instant.parse("2026-03-11T18:00:00Z"), ZoneId.of("UTC")),
                ZoneId.of("America/Fortaleza")
        );
        PessoaLocalizacaoIntelligenceSummaryService service = new PessoaLocalizacaoIntelligenceSummaryService(repository, timeService);

        Usuario executor = new Usuario();
        executor.setId(10L);
        executor.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);

        PessoaLocalizacaoConsultaGovernada critica = entity("corr-1", "CRITICO", 91, true, true, false, LocalDateTime.of(2026, 3, 11, 12, 0));
        PessoaLocalizacaoConsultaGovernada alta = entity("corr-2", "ALTO", 77, false, false, true, LocalDateTime.of(2026, 3, 10, 9, 30));

        when(repository.countByExecutorUserIdAndCanalConsultaAndCreatedAtGreaterThanEqual(eq(10L), eq(PessoaLocalizacaoCanalConsulta.DELEGADO), any(LocalDateTime.class)))
                .thenReturn(120L)
                .thenReturn(430L);
        when(repository.countByExecutorUserIdAndCanalConsultaAndRequerRevisaoTrueAndCreatedAtGreaterThanEqual(eq(10L), eq(PessoaLocalizacaoCanalConsulta.DELEGADO), any(LocalDateTime.class)))
                .thenReturn(17L);
        when(repository.countByExecutorUserIdAndCanalConsultaAndEnderecoEstritoLiberadoTrueAndCreatedAtGreaterThanEqual(eq(10L), eq(PessoaLocalizacaoCanalConsulta.DELEGADO), any(LocalDateTime.class)))
                .thenReturn(44L);
        when(repository.countByExecutorUserIdAndCanalConsultaAndPossuiContextoFormalFalseAndCreatedAtGreaterThanEqual(eq(10L), eq(PessoaLocalizacaoCanalConsulta.DELEGADO), any(LocalDateTime.class)))
                .thenReturn(9L);
        when(repository.countByExecutorUserIdAndCanalConsultaAndStepUpRequiredTrueAndStepUpSatisfiedFalseAndCreatedAtGreaterThanEqual(eq(10L), eq(PessoaLocalizacaoCanalConsulta.DELEGADO), any(LocalDateTime.class)))
                .thenReturn(4L);
        when(repository.findByExecutorUserIdAndCanalConsultaOrderByCreatedAtDesc(eq(10L), eq(PessoaLocalizacaoCanalConsulta.DELEGADO), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(2, Pageable.class);
                    return pageable.getPageSize() >= 200 ? List.of(critica, alta) : List.of(critica, alta);
                });

        var metricas = service.resumir(executor, PessoaLocalizacaoService.CanalConsulta.DELEGADO, 10);

        assertThat(metricas.consultasUltimas24h()).isEqualTo(120);
        assertThat(metricas.consultasUltimos7Dias()).isEqualTo(430);
        assertThat(metricas.consultasComRevisao()).isEqualTo(17);
        assertThat(metricas.consultasEnderecoEstrito()).isEqualTo(44);
        assertThat(metricas.consultasSemContextoFormal()).isEqualTo(9);
        assertThat(metricas.consultasStepUpPendentes()).isEqualTo(4);
        assertThat(metricas.posturaPredominante()).isEqualTo("ALTO");
        assertThat(metricas.exigeAtencaoOperacional()).isTrue();
        assertThat(metricas.consultasRecentes()).hasSize(2);
        assertThat(metricas.alertasOperacionais()).isNotEmpty();
    }

    private static PessoaLocalizacaoConsultaGovernada entity(String correlationId,
                                                             String postura,
                                                             int score,
                                                             boolean revisao,
                                                             boolean enderecoEstrito,
                                                             boolean contextoFormal,
                                                             LocalDateTime createdAt) {
        return PessoaLocalizacaoConsultaGovernada.builder()
                .correlationId(correlationId)
                .executorUserId(10L)
                .executorTipoUsuario(TipoUsuario.DELEGADO_POLICIA)
                .canalConsulta(PessoaLocalizacaoCanalConsulta.DELEGADO)
                .fundamento(PessoaLocalizacaoFundamento.INVESTIGACAO_POLICIAL_FORMAL)
                .referenciaProcedimental("IP-2026-1")
                .finalidade("investigação")
                .justificativaOperacional("justificativa")
                .cpfHash("hash")
                .cpfMascarado("***.***.***-**")
                .possuiContextoFormal(contextoFormal)
                .consultaSemProcessoAutorizada(!contextoFormal)
                .enderecoEstritoSolicitado(enderecoEstrito)
                .enderecoEstritoLiberado(enderecoEstrito)
                .nivelExposicao(enderecoEstrito ? "ESTRITO" : "MINIMIZADO")
                .posturaNivel(postura)
                .posturaScore(score)
                .requerRevisao(revisao)
                .modoLiberacao("FORMAL_MINIMIZADO")
                .stepUpRequired(revisao)
                .stepUpSatisfied(!revisao)
                .fontesConsultadas(2)
                .enderecosEncontrados(1)
                .restricoesEncontradas(1)
                .vinculosEncontrados(2)
                .alertasCount(1)
                .sinaisPostura("TESTE")
                .createdAt(createdAt)
                .build();
    }
}
