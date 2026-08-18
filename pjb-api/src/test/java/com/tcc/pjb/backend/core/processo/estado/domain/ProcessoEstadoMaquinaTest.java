package com.tcc.pjb.backend.core.processo.estado.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.estado.application.ProcessoEstadoApplicationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.processo.ProcessoEstadoLog;
import com.tcc.pjb.backend.model.repository.PainelSerieTemporalDiariaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoEstadoLogRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessoEstadoMaquinaTest {

    private final ProcessoEstadoMaquina maquina = new ProcessoEstadoMaquina();
    private ProcessoRepository processoRepository;
    private ProcessoEstadoLogRepository logRepository;
    private AuditLedgerService auditLedgerService;
    private PainelSerieTemporalDiariaRepository serieRepository;
    private ProcessoEstadoApplicationService appService;

    @BeforeEach
    void setUp() {
        processoRepository = mock(ProcessoRepository.class);
        logRepository = mock(ProcessoEstadoLogRepository.class);
        auditLedgerService = mock(AuditLedgerService.class);
        serieRepository = mock(PainelSerieTemporalDiariaRepository.class);
        when(serieRepository.findByChaveSerie(any())).thenReturn(java.util.Optional.empty());
        when(serieRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        appService = new ProcessoEstadoApplicationService(
                processoRepository, logRepository, auditLedgerService, serieRepository);
    }

    @Test
    void peticionadoParaAutuadoPermitida() {
        var result = maquina.avaliar(StatusProcesso.PETICIONADO, StatusProcesso.AUTUADO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void peticionadoParaSentenciadoNegada() {
        var result = maquina.avaliar(StatusProcesso.PETICIONADO, StatusProcesso.SENTENCIADO);
        assertInstanceOf(TransicaoEstadoResult.Negada.class, result);
    }

    @Test
    void autuadoParaDistribuidoPermitida() {
        var result = maquina.avaliar(StatusProcesso.AUTUADO, StatusProcesso.DISTRIBUIDO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void distribuidoParaConclusoJuizPermitida() {
        var result = maquina.avaliar(StatusProcesso.DISTRIBUIDO, StatusProcesso.CONCLUSO_JUIZ);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void sentenciadoParaTransitadoPermitida() {
        var result = maquina.avaliar(StatusProcesso.SENTENCIADO, StatusProcesso.TRANSITADO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void transitadoParaArquivadoPermitida() {
        var result = maquina.avaliar(StatusProcesso.TRANSITADO, StatusProcesso.ARQUIVADO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void arquivadoParaDistribuidoNegada() {
        var result = maquina.avaliar(StatusProcesso.ARQUIVADO, StatusProcesso.DISTRIBUIDO);
        assertInstanceOf(TransicaoEstadoResult.Negada.class, result);
    }

    @Test
    void suspensoParaConclusoJuizPermitida() {
        var result = maquina.avaliar(StatusProcesso.SUSPENSO, StatusProcesso.CONCLUSO_JUIZ);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void sobrestadoParaPautadoPermitida() {
        var result = maquina.avaliar(StatusProcesso.SOBRESTADO, StatusProcesso.PAUTADO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void emJulgamentoParaSentenciadoPermitida() {
        var result = maquina.avaliar(StatusProcesso.EM_JULGAMENTO, StatusProcesso.SENTENCIADO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void transicoesPossiveisDistribuidoContémConclusoJuiz() {
        Set<StatusProcesso> possiveis = maquina.transicoesPossiveis(StatusProcesso.DISTRIBUIDO);
        assertTrue(possiveis.contains(StatusProcesso.CONCLUSO_JUIZ));
    }

    @Test
    void transicoesPossiveisArquivadoVazio() {
        Set<StatusProcesso> possiveis = maquina.transicoesPossiveis(StatusProcesso.ARQUIVADO);
        assertTrue(possiveis.isEmpty());
    }

    @Test
    void negadaMotivoNaoNulo() {
        var result = maquina.avaliar(StatusProcesso.PETICIONADO, StatusProcesso.SENTENCIADO);
        assertInstanceOf(TransicaoEstadoResult.Negada.class, result);
        assertNotNull(((TransicaoEstadoResult.Negada) result).motivo());
    }

    @Test
    void transitarPersistLog() {
        Processo processo = mock(Processo.class);
        when(processo.getStatusProcesso()).thenReturn(StatusProcesso.PETICIONADO);
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processoRepository.save(any())).thenReturn(processo);

        appService.transitar(1L, StatusProcesso.AUTUADO, 99L, "autuação");

        verify(logRepository).save(any(ProcessoEstadoLog.class));
    }

    @Test
    void transitarEstadoInvalidoLancaExcecao() {
        Processo processo = mock(Processo.class);
        when(processo.getStatusProcesso()).thenReturn(StatusProcesso.PETICIONADO);
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

        assertThrows(IllegalStateException.class,
                () -> appService.transitar(1L, StatusProcesso.SENTENCIADO, 99L, "inválido"));
    }

    @Test
    void historicoRetornaEmOrdemCronologica() {
        when(processoRepository.findById(1L)).thenReturn(Optional.of(mock(Processo.class)));
        when(logRepository.findByProcessoIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        List<ProcessoEstadoLog> hist = appService.historico(1L);
        assertNotNull(hist);
    }

    @Test
    void paralisadoParaDistribuidoPermitida() {
        var result = maquina.avaliar(StatusProcesso.PARALISADO, StatusProcesso.DISTRIBUIDO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void extintoSemResolucaoParaArquivadoPermitida() {
        var result = maquina.avaliar(StatusProcesso.EXTINTO_SEM_RESOLUCAO, StatusProcesso.ARQUIVADO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void conclusoRelatorParaPautadoPermitida() {
        var result = maquina.avaliar(StatusProcesso.CONCLUSO_RELATOR, StatusProcesso.PAUTADO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }

    @Test
    void pautadoParaEmJulgamentoPermitida() {
        var result = maquina.avaliar(StatusProcesso.PAUTADO, StatusProcesso.EM_JULGAMENTO);
        assertInstanceOf(TransicaoEstadoResult.Permitida.class, result);
    }
}
