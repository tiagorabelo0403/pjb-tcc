package com.tcc.pjb.backend.service.ajuizamento;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.distribuicao.ProcessoInitialDistributionSnapshotService;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class AjuizamentoPostCommitOperationalEffectsServiceTest {

    private final AjuizamentoService ajuizamentoService = mock(AjuizamentoService.class);
    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine = mock(MapaCompetenciaDinamicoEngine.class);
    private final ProcessoInitialDistributionSnapshotService processoInitialDistributionSnapshotService = mock(ProcessoInitialDistributionSnapshotService.class);
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
                mapaCompetenciaDinamicoEngine,
                processoInitialDistributionSnapshotService,
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
        when(mapaCompetenciaDinamicoEngine.registrarDistribuicaoInicial(processo)).thenReturn(Optional.empty());

        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(99L).build());

        verify(ajuizamentoService).carregarProcesso(99L);
        verify(mapaCompetenciaDinamicoEngine).registrarDistribuicaoInicial(processo);
        verify(processoInitialDistributionSnapshotService).consolidar(processo);
        verify(prontuarioNacionalService).registrarProcessoAjuizado(processo);
        verify(federalismoJudicialEngine).registrarProcessoAjuizado(processo);
        verify(painelNacionalJusticaService).onProcessoAjuizado(processo);
        verify(radarPadroesService).analisarERegistrar(processo);
    }

    @Test
    void deveIgnorarEventoNuloOuSemProcesso() {
        service.onProcessoAjuizado(null);
        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(null).build());

        verifyNoInteractions(ajuizamentoService, mapaCompetenciaDinamicoEngine, processoInitialDistributionSnapshotService,
                prontuarioNacionalService, federalismoJudicialEngine, painelNacionalJusticaService, radarPadroesService);
    }

    @Test
    void deveManterFluxoQuandoUmColaboradorNaoBloqueanteFalhar() {
        Processo processo = Processo.builder().id(100L).build();
        when(ajuizamentoService.carregarProcesso(100L)).thenReturn(processo);
        when(mapaCompetenciaDinamicoEngine.registrarDistribuicaoInicial(processo)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("falha radar")).when(radarPadroesService).analisarERegistrar(processo);

        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(100L).build());

        verify(prontuarioNacionalService).registrarProcessoAjuizado(processo);
        verify(federalismoJudicialEngine).registrarProcessoAjuizado(processo);
        verify(painelNacionalJusticaService).onProcessoAjuizado(processo);
        verify(radarPadroesService).analisarERegistrar(processo);
    }
}
