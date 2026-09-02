package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.vertical.domain.ProcessoVerticalAggregate;
import com.tcc.pjb.backend.core.processo.vertical.estadual.civel.application.ProcessoVerticalCivelPrimeiroGrauApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.fazenda.application.ProcessoVerticalExecucaoFiscalFazendariaApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.penal.application.ProcessoVerticalPenalCustodiaApplicationService;
import org.junit.jupiter.api.Test;

class ProcessoSurfaceVerticalOrchestratorTest {

    private final ProcessoVerticalCivelPrimeiroGrauApplicationService civel = mock(ProcessoVerticalCivelPrimeiroGrauApplicationService.class);
    private final ProcessoVerticalPenalCustodiaApplicationService penal = mock(ProcessoVerticalPenalCustodiaApplicationService.class);
    private final ProcessoVerticalExecucaoFiscalFazendariaApplicationService fiscal = mock(ProcessoVerticalExecucaoFiscalFazendariaApplicationService.class);
    private final ProcessoSurfaceVerticalOrchestrator orchestrator = new ProcessoSurfaceVerticalOrchestrator(civel, penal, fiscal);

    @Test
    void tresVerticaisDelegam() {
        var c = mock(ProcessoVerticalAggregate.class);
        var p = mock(ProcessoVerticalAggregate.class);
        var f = mock(ProcessoVerticalAggregate.class);
        when(civel.detalhar(1L)).thenReturn(c);
        when(penal.detalhar(1L)).thenReturn(p);
        when(fiscal.detalhar(1L)).thenReturn(f);
        assertThat(orchestrator.civel(1L)).isSameAs(c);
        assertThat(orchestrator.penalCustodia(1L)).isSameAs(p);
        assertThat(orchestrator.execucaoFiscal(1L)).isSameAs(f);
    }
}
