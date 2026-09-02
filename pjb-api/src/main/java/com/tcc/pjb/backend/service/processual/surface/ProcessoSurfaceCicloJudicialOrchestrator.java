package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de ProcessoSurfaceFacadeService: ciclo judicial pós-primeiro grau --
 * família recursal (trilhas de recurso, caderno decisório de origem) e execução
 * (trilhas executivas, mandados, bloqueios).
 */
@Service
public class ProcessoSurfaceCicloJudicialOrchestrator {

    private final ProcessoRecursalApplicationService processoRecursalApplicationService;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;

    public ProcessoSurfaceCicloJudicialOrchestrator(ProcessoRecursalApplicationService processoRecursalApplicationService,
                                                     ProcessoExecucaoApplicationService processoExecucaoApplicationService) {
        this.processoRecursalApplicationService = Objects.requireNonNull(processoRecursalApplicationService);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
    }

    public ProcessoRecursalAggregate recursal(Long processoId) {
        return processoRecursalApplicationService.detalhar(processoId);
    }

    public ProcessoExecucaoAggregate execucao(Long processoId) {
        return processoExecucaoApplicationService.detalhar(processoId);
    }
}
