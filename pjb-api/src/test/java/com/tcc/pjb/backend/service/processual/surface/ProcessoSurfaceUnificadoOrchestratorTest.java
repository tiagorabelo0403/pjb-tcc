package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessoSurfaceUnificadoOrchestratorTest {

    private final ProcessoUnificadoApplicationService svc = mock(ProcessoUnificadoApplicationService.class);
    private final ProcessoSurfaceUnificadoOrchestrator orchestrator = new ProcessoSurfaceUnificadoOrchestrator(svc);

    @Test
    void metodosDelegam1a1() {
        var agg = mock(ProcessoUnificadoAggregate.class);
        var comp = mock(ProcessoUnificadoCompetencia.class);
        var diag = mock(ProcessoUnificadoDiagnostico.class);
        List<ProcessoUnificadoAto> atos = List.of(mock(ProcessoUnificadoAto.class), mock(ProcessoUnificadoAto.class));
        when(svc.detalhar(1L)).thenReturn(agg);
        when(svc.competencia(1L)).thenReturn(comp);
        when(svc.catalogoAtos(1L)).thenReturn(atos);
        when(svc.diagnosticar(1L)).thenReturn(diag);

        assertThat(orchestrator.detalhar(1L)).isSameAs(agg);
        assertThat(orchestrator.competencia(1L)).isSameAs(comp);
        assertThat(orchestrator.catalogoAtos(1L)).isSameAs(atos);
        assertThat(orchestrator.diagnosticar(1L)).isSameAs(diag);
    }
}
