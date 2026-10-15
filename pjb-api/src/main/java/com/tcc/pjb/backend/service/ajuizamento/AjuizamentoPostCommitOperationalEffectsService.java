package com.tcc.pjb.backend.service.ajuizamento;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
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
    private final ProntuarioNacionalService prontuarioNacionalService;
    private final FederalismoJudicialEngine federalismoJudicialEngine;
    private final PainelNacionalJusticaService painelNacionalJusticaService;
    private final RadarPadroesService radarPadroesService;
    private final TransactionTemplate postCommitTransactionTemplate;

    public AjuizamentoPostCommitOperationalEffectsService(AjuizamentoService ajuizamentoService,
                                                          ProntuarioNacionalService prontuarioNacionalService,
                                                          FederalismoJudicialEngine federalismoJudicialEngine,
                                                          PainelNacionalJusticaService painelNacionalJusticaService,
                                                          RadarPadroesService radarPadroesService,
                                                          PlatformTransactionManager transactionManager) {
        this.ajuizamentoService = Objects.requireNonNull(ajuizamentoService);
        this.prontuarioNacionalService = Objects.requireNonNull(prontuarioNacionalService);
        this.federalismoJudicialEngine = Objects.requireNonNull(federalismoJudicialEngine);
        this.painelNacionalJusticaService = Objects.requireNonNull(painelNacionalJusticaService);
        this.radarPadroesService = Objects.requireNonNull(radarPadroesService);
        TransactionTemplate template = new TransactionTemplate(Objects.requireNonNull(transactionManager));
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.postCommitTransactionTemplate = template;
    }

    @Order(3)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @PjbTransactionalBudget(operation = "ajuizamento.service.post-commit.persist", maxMillis = 2200, critical = true)
    public void onProcessoAjuizado(ProcessoAjuizadoEvent event) {
        if (event == null || event.getProcessoId() == null) {
            return;
        }
        Long processoId = event.getProcessoId();
        emTransacaoPropria(processoId, "Prontuario nacional", prontuarioNacionalService::registrarProcessoAjuizado);
        emTransacaoPropria(processoId, "Registro federativo", federalismoJudicialEngine::registrarProcessoAjuizado);
        emTransacaoPropria(processoId, "Painel nacional", painelNacionalJusticaService::onProcessoAjuizado);
        emTransacaoPropria(processoId, "Radar de padroes", radarPadroesService::analisarERegistrar);
    }

    private void emTransacaoPropria(Long processoId, String efeito, Consumer<Processo> acao) {
        try {
            postCommitTransactionTemplate.executeWithoutResult(status -> acao.accept(ajuizamentoService.carregarProcesso(processoId)));
        } catch (Exception ex) {
            log.warn("{} nao bloqueante falhou. processoId={} erro={}", efeito, processoId, ex.getMessage());
        }
    }
}
