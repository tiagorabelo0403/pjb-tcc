package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.hardening.application.ProcessoHardeningFinalApplicationService;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloNotificacaoApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloNotificacaoAggregate;
import org.junit.jupiter.api.Test;

class ProcessoSurfaceSigiloOrchestratorTest {

    private final ProcessoSigiloApplicationService sig = mock(ProcessoSigiloApplicationService.class);
    private final ProcessoHardeningFinalApplicationService hardening = mock(ProcessoHardeningFinalApplicationService.class);
    private final ProcessoSigiloInteligenteApplicationService inteligente = mock(ProcessoSigiloInteligenteApplicationService.class);
    private final ProcessoSigiloNotificacaoApplicationService notif = mock(ProcessoSigiloNotificacaoApplicationService.class);
    private final ProcessoSurfaceSigiloOrchestrator orchestrator = new ProcessoSurfaceSigiloOrchestrator(sig, hardening, inteligente, notif);

    @Test
    void sigiloEHardeningDelegam() {
        var s = mock(ProcessoSigiloAggregate.class);
        var h = mock(ProcessoHardeningAggregate.class);
        when(sig.detalhar(1L)).thenReturn(s);
        when(hardening.detalhar(1L)).thenReturn(h);
        assertThat(orchestrator.sigilo(1L)).isSameAs(s);
        assertThat(orchestrator.hardening(1L)).isSameAs(h);
    }

    @Test
    void sigiloInteligenteDelega() {
        var si = mock(ProcessoSigiloInteligenteAggregate.class);
        when(inteligente.avaliar(1L)).thenReturn(si);
        assertThat(orchestrator.sigiloInteligente(1L)).isSameAs(si);
    }

    @Test
    void notificacoesDelegamPlanejarEDisparar() {
        var plan = mock(ProcessoSigiloNotificacaoAggregate.class);
        var disp = mock(ProcessoSigiloNotificacaoAggregate.class);
        when(notif.planejar(1L)).thenReturn(plan);
        when(notif.notificar(1L)).thenReturn(disp);
        assertThat(orchestrator.planejarSigiloNotificacoes(1L)).isSameAs(plan);
        assertThat(orchestrator.dispararSigiloNotificacoes(1L)).isSameAs(disp);
    }
}
