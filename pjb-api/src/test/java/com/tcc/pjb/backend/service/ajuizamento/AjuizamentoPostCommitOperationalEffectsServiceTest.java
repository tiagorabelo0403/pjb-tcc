package com.tcc.pjb.backend.service.ajuizamento;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class AjuizamentoPostCommitOperationalEffectsServiceTest {

    private final AjuizamentoService ajuizamentoService = mock(AjuizamentoService.class);
    private final ProntuarioNacionalService prontuarioNacionalService = mock(ProntuarioNacionalService.class);
    private final FederalismoJudicialEngine federalismoJudicialEngine = mock(FederalismoJudicialEngine.class);
    private final PainelNacionalJusticaService painelNacionalJusticaService = mock(PainelNacionalJusticaService.class);
    private final RadarPadroesService radarPadroesService = mock(RadarPadroesService.class);
    private final PlatformTransactionManager transactionManager = new AbstractPlatformTransactionManager() {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    };

    private AjuizamentoPostCommitOperationalEffectsService service;

    @BeforeEach
    void setUp() {
        service = new AjuizamentoPostCommitOperationalEffectsService(
                ajuizamentoService,
                prontuarioNacionalService,
                federalismoJudicialEngine,
                painelNacionalJusticaService,
                radarPadroesService,
                transactionManager
        );
    }

    @Test
    void deveAplicarEfeitosOperacionaisPosCommit() {
        Processo processo = Processo.builder().id(99L).numeroUnificado("0009001-11.2026.8.06.0001").build();
        when(ajuizamentoService.carregarProcesso(99L)).thenReturn(processo);

        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(99L).build());

        verify(ajuizamentoService, times(4)).carregarProcesso(99L);
        verify(prontuarioNacionalService).registrarProcessoAjuizado(processo);
        verify(federalismoJudicialEngine).registrarProcessoAjuizado(processo);
        verify(painelNacionalJusticaService).onProcessoAjuizado(processo);
        verify(radarPadroesService).analisarERegistrar(processo);
    }

    @Test
    void deveIgnorarEventoNuloOuSemProcesso() {
        service.onProcessoAjuizado(null);
        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(null).build());

        verifyNoInteractions(ajuizamentoService, prontuarioNacionalService, federalismoJudicialEngine, painelNacionalJusticaService, radarPadroesService);
    }

    @Test
    void falhaDeUmEfeitoNoMeioDaSequenciaNaoImpedeOsSeguintes() {
        Processo processo = Processo.builder().id(100L).build();
        when(ajuizamentoService.carregarProcesso(100L)).thenReturn(processo);
        doThrow(new IllegalStateException("falha prontuario")).when(prontuarioNacionalService).registrarProcessoAjuizado(processo);
        doThrow(new IllegalStateException("falha painel")).when(painelNacionalJusticaService).onProcessoAjuizado(processo);

        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(100L).build());

        verify(prontuarioNacionalService).registrarProcessoAjuizado(processo);
        verify(federalismoJudicialEngine).registrarProcessoAjuizado(processo);
        verify(painelNacionalJusticaService).onProcessoAjuizado(processo);
        verify(radarPadroesService).analisarERegistrar(processo);
    }

    @Test
    void falhaAoCarregarOProcessoEmUmEfeitoNaoImpedeOsSeguintes() {
        Processo processo = Processo.builder().id(101L).build();
        when(ajuizamentoService.carregarProcesso(101L))
                .thenThrow(new IllegalStateException("processo indisponivel"))
                .thenReturn(processo);

        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(101L).build());

        verify(prontuarioNacionalService, never()).registrarProcessoAjuizado(any());
        verify(federalismoJudicialEngine).registrarProcessoAjuizado(processo);
        verify(painelNacionalJusticaService).onProcessoAjuizado(processo);
        verify(radarPadroesService).analisarERegistrar(processo);
    }
}
