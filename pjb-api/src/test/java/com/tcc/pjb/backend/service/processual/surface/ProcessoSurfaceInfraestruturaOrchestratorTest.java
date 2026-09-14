package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import org.junit.jupiter.api.Test;

class ProcessoSurfaceInfraestruturaOrchestratorTest {

    private final ProcessoIntegracaoApplicationService integ = mock(ProcessoIntegracaoApplicationService.class);
    private final ProcessoMigracaoApplicationService migr = mock(ProcessoMigracaoApplicationService.class);
    private final ProcessoOperacaoApplicationService oper = mock(ProcessoOperacaoApplicationService.class);
    private final ProcessoSurfaceInfraestruturaOrchestrator orchestrator = new ProcessoSurfaceInfraestruturaOrchestrator(integ, migr, oper);

    @Test
    void metodos3Delegam() {
        var i = mock(ProcessoIntegracaoAggregate.class);
        var m = mock(ProcessoMigracaoAggregate.class);
        var o = mock(ProcessoOperacaoAggregate.class);
        when(integ.detalhar(1L)).thenReturn(i);
        when(migr.detalhar(1L)).thenReturn(m);
        when(oper.detalhar(1L)).thenReturn(o);
        assertThat(orchestrator.integracoes(1L)).isSameAs(i);
        assertThat(orchestrator.migracao(1L)).isSameAs(m);
        assertThat(orchestrator.operacao(1L)).isSameAs(o);
    }
}
