package com.tcc.pjb.backend.service.ajuizamento;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.distribuicao.ProcessoInitialDistributionSnapshotService;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@PjbPublicApi(module = PjbModuleId.AJUIZAMENTO)
public class AjuizamentoPostCommitOperationalEffectsService {

    private static final Logger log = LoggerFactory.getLogger(AjuizamentoPostCommitOperationalEffectsService.class);

    private final AjuizamentoService ajuizamentoService;
    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;
    private final ProcessoInitialDistributionSnapshotService processoInitialDistributionSnapshotService;
    private final ProntuarioNacionalService prontuarioNacionalService;
    private final FederalismoJudicialEngine federalismoJudicialEngine;
    private final PainelNacionalJusticaService painelNacionalJusticaService;
    private final RadarPadroesService radarPadroesService;
    private final TransactionTemplate postCommitTransactionTemplate;

    public AjuizamentoPostCommitOperationalEffectsService(AjuizamentoService ajuizamentoService,
                                                          MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine,
                                                          ProcessoInitialDistributionSnapshotService processoInitialDistributionSnapshotService,
                                                          ProntuarioNacionalService prontuarioNacionalService,
                                                          FederalismoJudicialEngine federalismoJudicialEngine,
                                                          PainelNacionalJusticaService painelNacionalJusticaService,
                                                          RadarPadroesService radarPadroesService,
                                                          PlatformTransactionManager transactionManager) {
        this.ajuizamentoService = Objects.requireNonNull(ajuizamentoService);
        this.mapaCompetenciaDinamicoEngine = Objects.requireNonNull(mapaCompetenciaDinamicoEngine);
        this.processoInitialDistributionSnapshotService = Objects.requireNonNull(processoInitialDistributionSnapshotService);
        this.prontuarioNacionalService = Objects.requireNonNull(prontuarioNacionalService);
        this.federalismoJudicialEngine = Objects.requireNonNull(federalismoJudicialEngine);
        this.painelNacionalJusticaService = Objects.requireNonNull(painelNacionalJusticaService);
        this.radarPadroesService = Objects.requireNonNull(radarPadroesService);
        TransactionTemplate template = new TransactionTemplate(Objects.requireNonNull(transactionManager));
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.postCommitTransactionTemplate = template;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @PjbTransactionalBudget(operation = "ajuizamento.service.post-commit.persist", maxMillis = 2200, critical = true)
    public void onProcessoAjuizado(ProcessoAjuizadoEvent event) {
        if (event == null || event.getProcessoId() == null) {
            return;
        }
        Processo processo = ajuizamentoService.carregarProcesso(event.getProcessoId());
        registrarCompetenciaSafely(processo);
        consolidarDistribuicaoInicialSafely(processo);
        registrarProntuarioSafely(processo);
        registrarFederalismoSafely(processo);
        registrarPainelSafely(processo);
        registrarRadarSafely(processo);
    }

    private void consolidarDistribuicaoInicialSafely(Processo processo) {
        try {
            processoInitialDistributionSnapshotService.consolidar(processo);
        } catch (Exception ex) {
            log.warn("Snapshot inicial de distribuicao nao bloqueante falhou. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
    }

    private void registrarCompetenciaSafely(Processo processo) {
        try {
            mapaCompetenciaDinamicoEngine.registrarDistribuicaoInicial(processo);
        } catch (Exception ex) {
            log.warn("Distribuicao dinamica de competencia nao bloqueante falhou. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
    }

    private void registrarProntuarioSafely(Processo processo) {
        try {
            prontuarioNacionalService.registrarProcessoAjuizado(processo);
        } catch (Exception ex) {
            log.warn("Prontuario nacional nao bloqueante falhou. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
    }

    private void registrarPainelSafely(Processo processo) {
        try {
            painelNacionalJusticaService.onProcessoAjuizado(processo);
        } catch (Exception ex) {
            log.warn("Painel nacional nao bloqueante falhou. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
    }

    private void registrarFederalismoSafely(Processo processo) {
        try {
            postCommitTransactionTemplate.executeWithoutResult(status -> federalismoJudicialEngine.registrarProcessoAjuizado(processo));
        } catch (Exception ex) {
            log.warn("Registro federativo nao bloqueante falhou. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
    }

    private void registrarRadarSafely(Processo processo) {
        try {
            radarPadroesService.analisarERegistrar(processo);
        } catch (Exception ex) {
            log.warn("Radar de padroes nao bloqueante falhou. processoId={} erro={}", processo != null ? processo.getId() : null, ex.getMessage());
        }
    }
}
