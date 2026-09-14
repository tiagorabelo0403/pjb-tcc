package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de ProcessoSurfaceFacadeService: infraestrutura de plataforma --
 * integrações externas (conectores + shadow), migração (readiness + cutover) e
 * operação (resiliência + observabilidade + saturação).
 */
@Service
public class ProcessoSurfaceInfraestruturaOrchestrator {

    private final ProcessoIntegracaoApplicationService processoIntegracaoApplicationService;
    private final ProcessoMigracaoApplicationService processoMigracaoApplicationService;
    private final ProcessoOperacaoApplicationService processoOperacaoApplicationService;

    public ProcessoSurfaceInfraestruturaOrchestrator(ProcessoIntegracaoApplicationService processoIntegracaoApplicationService,
                                                      ProcessoMigracaoApplicationService processoMigracaoApplicationService,
                                                      ProcessoOperacaoApplicationService processoOperacaoApplicationService) {
        this.processoIntegracaoApplicationService = Objects.requireNonNull(processoIntegracaoApplicationService);
        this.processoMigracaoApplicationService = Objects.requireNonNull(processoMigracaoApplicationService);
        this.processoOperacaoApplicationService = Objects.requireNonNull(processoOperacaoApplicationService);
    }

    public ProcessoIntegracaoAggregate integracoes(Long processoId) {
        return processoIntegracaoApplicationService.detalhar(processoId);
    }

    public ProcessoMigracaoAggregate migracao(Long processoId) {
        return processoMigracaoApplicationService.detalhar(processoId);
    }

    public ProcessoOperacaoAggregate operacao(Long processoId) {
        return processoOperacaoApplicationService.detalhar(processoId);
    }
}
