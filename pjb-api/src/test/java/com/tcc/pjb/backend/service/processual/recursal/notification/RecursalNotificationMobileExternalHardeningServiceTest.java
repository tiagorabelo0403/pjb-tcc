package com.tcc.pjb.backend.service.processual.recursal.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobileExternalDeliveryResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobileHardeningRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobilePostureResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyRequest;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalOperationalSurfaceProjectionSupport;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalNotificationMobileExternalHardeningServiceTest {

    private final RecursalAutomationService automationService = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
    private final RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
    private final RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
    private final RecursalIntelligenceSurfaceService intelligenceSurfaceService = new RecursalIntelligenceSurfaceService(projectionSupport);
    private final RecursalNotificationPreferenceFederationService preferenceFederationService = new RecursalNotificationPreferenceFederationService(intelligenceSurfaceService);
    private final RecursalNotificationMobileExternalHardeningService service = new RecursalNotificationMobileExternalHardeningService(preferenceFederationService);

    @Test
    void deveAprovarPosturaMobileSoberanaQuandoAtestacaoEstaFechada() {
        RecursalNotificationMobilePostureResponse response = service.mobilePosture(baseRequest(true));

        assertThat(response.titulo()).isEqualTo("POSTURA_MOBILE_SOBERANA");
        assertThat(response.posturaStatus()).isEqualTo("POSTURA_APROVADA");
        assertThat(response.posturaScore()).isGreaterThanOrEqualTo(80);
        assertThat(response.canalMobileAprovado()).isTrue();
    }

    @Test
    void deveBloquearEntregaQuandoPosturaMobileNaoFecha() {
        RecursalNotificationMobileExternalDeliveryResponse response = service.hardenedDelivery(baseRequest(false));

        assertThat(response.titulo()).isEqualTo("ENDURECIMENTO_MOBILE_EXTERNO_SOBERANO");
        assertThat(response.statusEntrega()).isEqualTo("ENTREGA_MOBILE_BLOQUEADA");
        assertThat(response.entregaSoberanaAprovada()).isFalse();
        assertThat(response.canalEntregaExterna()).isEqualTo("INBOX_OPERACIONAL");
    }

    private RecursalNotificationMobileHardeningRequest baseRequest(boolean approved) {
        return new RecursalNotificationMobileHardeningRequest(
                new RecursalNotificationPreferencePolicyRequest(
                        new RecursalNotificationGovernanceRequest(
                                baseAutomationRequest(),
                                "0001234-56.2026.8.13.0001",
                                "USR-001",
                                "ADVOGADO",
                                false,
                                true,
                                true,
                                true,
                                6,
                                "TOKEN-ABC"
                        ),
                        true,
                        true,
                        true,
                        true,
                        false,
                        true,
                        true,
                        "dominio.soberano.externo"
                ),
                approved,
                approved,
                approved,
                approved,
                approved,
                approved,
                approved,
                approved ? "IOS" : "ANDROID"
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
