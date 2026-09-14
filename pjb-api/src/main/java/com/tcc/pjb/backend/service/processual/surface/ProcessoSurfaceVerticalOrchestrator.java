package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalAggregate;
import com.tcc.pjb.backend.core.processo.vertical.estadual.civel.application.ProcessoVerticalCivelPrimeiroGrauApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.fazenda.application.ProcessoVerticalExecucaoFiscalFazendariaApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.penal.application.ProcessoVerticalPenalCustodiaApplicationService;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de ProcessoSurfaceFacadeService: cortes verticais estaduais --
 * fatia cível de primeiro grau, custódia penal e execução fiscal fazendária.
 * Cada vertical entrega o mesmo shape agregado ({@link ProcessoVerticalAggregate}).
 */
@Service
public class ProcessoSurfaceVerticalOrchestrator {

    private final ProcessoVerticalCivelPrimeiroGrauApplicationService processoVerticalCivelPrimeiroGrauApplicationService;
    private final ProcessoVerticalPenalCustodiaApplicationService processoVerticalPenalCustodiaApplicationService;
    private final ProcessoVerticalExecucaoFiscalFazendariaApplicationService processoVerticalExecucaoFiscalFazendariaApplicationService;

    public ProcessoSurfaceVerticalOrchestrator(ProcessoVerticalCivelPrimeiroGrauApplicationService processoVerticalCivelPrimeiroGrauApplicationService,
                                                ProcessoVerticalPenalCustodiaApplicationService processoVerticalPenalCustodiaApplicationService,
                                                ProcessoVerticalExecucaoFiscalFazendariaApplicationService processoVerticalExecucaoFiscalFazendariaApplicationService) {
        this.processoVerticalCivelPrimeiroGrauApplicationService = Objects.requireNonNull(processoVerticalCivelPrimeiroGrauApplicationService);
        this.processoVerticalPenalCustodiaApplicationService = Objects.requireNonNull(processoVerticalPenalCustodiaApplicationService);
        this.processoVerticalExecucaoFiscalFazendariaApplicationService = Objects.requireNonNull(processoVerticalExecucaoFiscalFazendariaApplicationService);
    }

    public ProcessoVerticalAggregate civel(Long processoId) {
        return processoVerticalCivelPrimeiroGrauApplicationService.detalhar(processoId);
    }

    public ProcessoVerticalAggregate penalCustodia(Long processoId) {
        return processoVerticalPenalCustodiaApplicationService.detalhar(processoId);
    }

    public ProcessoVerticalAggregate execucaoFiscal(Long processoId) {
        return processoVerticalExecucaoFiscalFazendariaApplicationService.detalhar(processoId);
    }
}
