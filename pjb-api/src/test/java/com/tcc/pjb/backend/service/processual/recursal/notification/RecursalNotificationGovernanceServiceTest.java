package com.tcc.pjb.backend.service.processual.recursal.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobilePreviewResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationScienceResponse;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalOperationalSurfaceProjectionSupport;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalNotificationGovernanceServiceTest {

    private final RecursalAutomationService automationService = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
    private final RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
    private final RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
    private final RecursalIntelligenceSurfaceService intelligenceSurfaceService = new RecursalIntelligenceSurfaceService(projectionSupport);
    private final RecursalNotificationGovernanceService governanceService = new RecursalNotificationGovernanceService(intelligenceSurfaceService);

    @Test
    void deveProjetarPreviewMobileSemSchedulerParalelo() {
        RecursalNotificationMobilePreviewResponse response = governanceService.mobilePreview(baseRequest(false, true, false, true, 4, "ADVOGADO"));

        assertThat(response.eixo()).isEqualTo("SUITE_NOTIFICACIONAL_RECURSAL_GOVERNADA");
        assertThat(response.titulo()).isEqualTo("PREVIEW_MOBILE_PENDENCIAS_RECURSAIS");
        assertThat(response.canalPrioritario()).isEqualTo("PUSH_GOVERNADO");
        assertThat(response.janelaCriticidade()).isEqualTo("JANELA_CRITICIDADE_IMEDIATA");
        assertThat(response.rotasRelacionadas())
                .contains("/api/v1/calendar/notification-preview?from={from}&to={to}&processoId={processoId}", "/api/v1/processual/recursal/analytics/notifica-pendencias", "/api/v1/processual/recursal/analytics/mobile-acompanhamento");
    }

    @Test
    void deveProjetarGovernancaNotificacionalComReusoDePreferenciasGlobais() {
        RecursalNotificationGovernanceResponse response = governanceService.governance(baseRequest(true, false, true, true, 18, "SECRETARIA"));

        assertThat(response.titulo()).isEqualTo("GOVERNANCA_NOTIFICACIONAL_RECURSAL");
        assertThat(response.statusFluxo()).isEqualTo("FLUXO_GOVERNADO");
        assertThat(response.schedulerParaleloPermitido()).isFalse();
        assertThat(response.executorParaleloPermitido()).isFalse();
        assertThat(response.politicasAplicadas())
                .contains("SEM_SCHEDULER_PARALELO", "SEM_EXECUTOR_PARALELO", "REUSAR_PREFERENCIAS_GLOBAIS");
    }

    @Test
    void deveProjetarCienciaRecursalComRastreamentoMulticanal() {
        RecursalNotificationScienceResponse response = governanceService.science(baseRequest(false, false, true, true, 12, "MP"));

        assertThat(response.titulo()).isEqualTo("CIENCIA_NOTIFICACIONAL_RECURSAL");
        assertThat(response.statusCiencia()).isEqualTo("CIENCIA_REGISTRADA");
        assertThat(response.cienciaRegistrada()).isTrue();
        assertThat(response.rotaTrackingGif()).isEqualTo("/api/v1/notificacoes/track/{token}.gif");
        assertThat(response.rotaTrackingCiencia()).isEqualTo("/api/v1/notificacoes/track/{token}/ciencia");
    }

    private RecursalNotificationGovernanceRequest baseRequest(boolean sigiloso,
                                                              boolean urgente,
                                                              boolean mobileAtivo,
                                                              boolean exigeCiencia,
                                                              int prazoCriticoHoras,
                                                              String perfilDestino) {
        return new RecursalNotificationGovernanceRequest(
                baseAutomationRequest(),
                "0001234-56.2026.8.13.0001",
                "USR-001",
                perfilDestino,
                sigiloso,
                urgente,
                mobileAtivo,
                exigeCiencia,
                prazoCriticoHoras,
                "TOKEN-ABC"
        );
    }

    private RecursalAutomationRequest baseAutomationRequest() {
        return new RecursalAutomationRequest(
                "ACORDAO",
                "REFORMAR",
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                Set.of(),
                "ESTADUAL",
                "CIVIL",
                false,
                false,
                "ADVOGADO",
                true,
                false
        );
    }
}
