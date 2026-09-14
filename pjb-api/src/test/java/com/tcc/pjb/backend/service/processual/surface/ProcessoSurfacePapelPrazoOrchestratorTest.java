package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelAggregate;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import org.junit.jupiter.api.Test;

class ProcessoSurfacePapelPrazoOrchestratorTest {

    private final ProcessoPapelApplicationService papel = mock(ProcessoPapelApplicationService.class);
    private final ProcessoPrazoApplicationService prazo = mock(ProcessoPrazoApplicationService.class);
    private final ProcessoSurfacePapelPrazoOrchestrator orchestrator = new ProcessoSurfacePapelPrazoOrchestrator(papel, prazo);

    @Test
    void papelDelegaAgregadoEPerfil() {
        var agg = mock(ProcessoPapelAggregate.class);
        var perfil = mock(ProcessoPapelPerfil.class);
        when(papel.detalhar(1L)).thenReturn(agg);
        when(papel.detalharPerfil(1L, "ADV")).thenReturn(perfil);
        assertThat(orchestrator.papeis(1L)).isSameAs(agg);
        assertThat(orchestrator.perfil(1L, "ADV")).isSameAs(perfil);
    }

    @Test
    void prazoDelegaAgregadoECalculo() {
        var agg = mock(ProcessoPrazoAggregate.class);
        var marco = mock(ProcessoPrazoMarco.class);
        when(prazo.detalhar(2L)).thenReturn(agg);
        when(prazo.calcular(2L, NationalPrazoEngine.TipoPrazo.CONTESTACAO)).thenReturn(marco);
        assertThat(orchestrator.prazos(2L)).isSameAs(agg);
        assertThat(orchestrator.calcularPrazo(2L, NationalPrazoEngine.TipoPrazo.CONTESTACAO)).isSameAs(marco);
    }
}
