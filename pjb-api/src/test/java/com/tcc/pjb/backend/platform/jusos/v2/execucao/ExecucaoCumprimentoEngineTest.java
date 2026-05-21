package com.tcc.pjb.backend.platform.jusos.v2.execucao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ExecucaoCumprimentoEngineTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final AuditLedgerService auditLedger = mock(AuditLedgerService.class);
    private final ExecucaoCumprimentoPlanningSupport planningSupport = mock(ExecucaoCumprimentoPlanningSupport.class);
    private final ExecucaoCumprimentoSimulationSupport simulationSupport = mock(ExecucaoCumprimentoSimulationSupport.class);
    private final UiHistoryService uiHistoryService = mock(UiHistoryService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private ExecucaoCumprimentoEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ExecucaoCumprimentoEngine(
                processoRepository, auditLedger,
                planningSupport, simulationSupport,
                uiHistoryService, currentUserService, eventPublisher);
    }

    @Test
    void deveLancarExcecaoQuandoProcessoNaoEncontradoEmAtivarExecucao() {
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engine.ativarExecucao(99L,
                ExecucaoCumprimentoEngine.TituloExecutivo.SENTENCA_CONDENATORIA,
                new BigDecimal("30000.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveLancarNullPointerQuandoProcessoIdNuloEmAtivarExecucao() {
        assertThatThrownBy(() -> engine.ativarExecucao(null,
                ExecucaoCumprimentoEngine.TituloExecutivo.SENTENCA_CONDENATORIA,
                BigDecimal.TEN))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveLancarNullPointerQuandoTituloNuloEmAtivarExecucao() {
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processoBase()));

        assertThatThrownBy(() -> engine.ativarExecucao(1L, null, BigDecimal.TEN))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveAtivarExecucaoAtualizandoFaseStatusEPublicandoEvento() {
        Processo processo = processoBase();
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.TRANSITO_EM_JULGADO);

        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(processoRepository.save(any())).thenReturn(processo);
        when(planningSupport.montarPlano(any(), any(), any())).thenReturn(planoBase());
        when(planningSupport.construirMensagemMudancaExecucao(any(), any(), anyString()))
                .thenReturn("Execução iniciada.");
        when(currentUserService.getOrNull()).thenReturn(null);

        ExecucaoCumprimentoEngine.ResultadoAtivacaoExecucao resultado = engine.ativarExecucao(
                1L,
                ExecucaoCumprimentoEngine.TituloExecutivo.SENTENCA_CONDENATORIA,
                new BigDecimal("45000.00"));

        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.processoId()).isEqualTo(1L);
        assertThat(resultado.faseAtual()).isEqualTo(FaseProcessual.EXECUCAO);
        assertThat(resultado.statusAtual()).isEqualTo(StatusProcesso.CUMPRIMENTO_SENTENCA);
        verify(eventPublisher).publishEvent(any(ExecucaoCumprimentoEngine.ExecucaoAtualizadaEvent.class));
        verify(auditLedger).appendSafely(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deveSimularPenhoraEDelegarParaSimulationSupport() {
        ExecucaoCumprimentoEngine.ResultadoPenhora resultadoEsperado = new ExecucaoCumprimentoEngine.ResultadoPenhora(
                UUID.randomUUID(),
                ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA,
                new BigDecimal("30000.00"),
                true,
                85,
                List.of("conta corrente TJCE"),
                List.of(),
                List.of());

        when(processoRepository.findById(1L)).thenReturn(Optional.of(processoBase()));
        when(simulationSupport.simularPenhora(any(), any(), any(), any())).thenReturn(resultadoEsperado);

        ExecucaoCumprimentoEngine.ResultadoPenhora resultado = engine.simularPenhora(
                1L,
                ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA,
                new BigDecimal("30000.00"));

        assertThat(resultado).isSameAs(resultadoEsperado);
        assertThat(resultado.suficiente()).isTrue();
        verify(simulationSupport).simularPenhora(any(), any(), any(), any());
    }

    @Test
    void deveGerarPainelDelegandoParaSimulationSupport() {
        Processo processo = processoBase();
        ExecucaoCumprimentoEngine.PlanoExecucao plano = planoBase();
        ExecucaoCumprimentoEngine.PainelExecucao painelEsperado = new ExecucaoCumprimentoEngine.PainelExecucao(
                1L, "0001234-56.2026.8.06.0001",
                new BigDecimal("45000.00"),
                2, 1, 1, false,
                List.of(), List.of(), List.of("Verificar penhora de imóvel"));

        when(simulationSupport.gerarPainel(any(), any(), any())).thenReturn(painelEsperado);

        ExecucaoCumprimentoEngine.PainelExecucao painel = engine.gerarPainel(processo, plano, List.of());

        assertThat(painel).isSameAs(painelEsperado);
        verify(simulationSupport).gerarPainel(any(), any(), any());
    }

    private Processo processoBase() {
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setNumeroUnificado("0001234-56.2026.8.06.0001");
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setValorCausa(new BigDecimal("45000.00"));
        return processo;
    }

    private ExecucaoCumprimentoEngine.PlanoExecucao planoBase() {
        ExecucaoCumprimentoEngine.ComplianceExecucao compliance = new ExecucaoCumprimentoEngine.ComplianceExecucao(
                true, true, false, false, false, false,
                List.of(), List.of(), List.of());
        ExecucaoCumprimentoEngine.MatrizExpropriacao matriz = new ExecucaoCumprimentoEngine.MatrizExpropriacao(
                70, 60, 40,
                new BigDecimal("45000.00"),
                List.of(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA),
                List.of(),
                List.of("Devedor com renda"),
                List.of());
        return new ExecucaoCumprimentoEngine.PlanoExecucao(
                1L,
                ExecucaoCumprimentoEngine.TituloExecutivo.SENTENCA_CONDENATORIA,
                new BigDecimal("45000.00"),
                new BigDecimal("47250.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("4725.00"),
                List.of(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA),
                List.of("Intimar devedor"),
                List.of(),
                "Art. 513 CPC — Cumprimento de sentença",
                true, 15, false,
                FaseProcessual.EXECUCAO,
                StatusProcesso.CUMPRIMENTO_SENTENCA,
                null,
                compliance,
                matriz,
                List.of("Confirmar bloqueio SISBAJUD"),
                List.of());
    }
}
