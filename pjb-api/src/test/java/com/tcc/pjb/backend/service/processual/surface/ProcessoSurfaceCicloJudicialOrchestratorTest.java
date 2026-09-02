package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import org.junit.jupiter.api.Test;

class ProcessoSurfaceCicloJudicialOrchestratorTest {

    private final ProcessoRecursalApplicationService recursal = mock(ProcessoRecursalApplicationService.class);
    private final ProcessoExecucaoApplicationService execucao = mock(ProcessoExecucaoApplicationService.class);
    private final ProcessoSurfaceCicloJudicialOrchestrator orchestrator = new ProcessoSurfaceCicloJudicialOrchestrator(recursal, execucao);

    @Test
    void recursalDelega() {
        var agg = mock(ProcessoRecursalAggregate.class);
        when(recursal.detalhar(42L)).thenReturn(agg);
        assertThat(orchestrator.recursal(42L)).isSameAs(agg);
    }

    @Test
    void execucaoDelega() {
        var agg = mock(ProcessoExecucaoAggregate.class);
        when(execucao.detalhar(42L)).thenReturn(agg);
        assertThat(orchestrator.execucao(42L)).isSameAs(agg);
    }
}
