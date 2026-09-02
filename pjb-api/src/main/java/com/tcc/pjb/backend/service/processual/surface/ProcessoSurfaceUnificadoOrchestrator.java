package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de ProcessoSurfaceFacadeService: visão unificada do processo --
 * detalhamento agregado, competência, catálogo de atos e diagnóstico ponta a ponta.
 */
@Service
public class ProcessoSurfaceUnificadoOrchestrator {

    private final ProcessoUnificadoApplicationService processoUnificadoApplicationService;

    public ProcessoSurfaceUnificadoOrchestrator(ProcessoUnificadoApplicationService processoUnificadoApplicationService) {
        this.processoUnificadoApplicationService = Objects.requireNonNull(processoUnificadoApplicationService);
    }

    public ProcessoUnificadoAggregate detalhar(Long processoId) {
        return processoUnificadoApplicationService.detalhar(processoId);
    }

    public ProcessoUnificadoCompetencia competencia(Long processoId) {
        return processoUnificadoApplicationService.competencia(processoId);
    }

    public List<ProcessoUnificadoAto> catalogoAtos(Long processoId) {
        return processoUnificadoApplicationService.catalogoAtos(processoId);
    }

    public ProcessoUnificadoDiagnostico diagnosticar(Long processoId) {
        return processoUnificadoApplicationService.diagnosticar(processoId);
    }
}
