package com.tcc.pjb.backend.service.processual.recursal.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationFederatedDeliveryResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyResponse;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalOperationalSurfaceProjectionSupport;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalNotificationPreferenceFederationServiceTest {

    private final RecursalAutomationService automationService = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
    private final RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
    private final RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
    private final RecursalIntelligenceSurfaceService intelligenceSurfaceService = new RecursalIntelligenceSurfaceService(projectionSupport);
    private final RecursalNotificationPreferenceFederationService service = new RecursalNotificationPreferenceFederationService(intelligenceSurfaceService);

    @Test
    void deveProjetarPreferenciasFinasPorPerfilECanal() {
        RecursalNotificationPreferencePolicyResponse response = service.preferences(baseRequest(false, true, true, true, false, false, true, false, "ADVOGADO"));

        assertThat(response.titulo()).isEqualTo("PREFERENCIAS_FINAS_RECURSAIS");
        assertThat(response.canalPreferencial()).isEqualTo("PUSH_GOVERNADO");
        assertThat(response.politicaPerfil()).isEqualTo("POLITICA_PERFIL_REPRESENTACAO_TECNICA");
        assertThat(response.preferenciasAplicadas()).contains("REUSAR_PREFERENCIAS_POR_CANAL");
        assertThat(response.rotasRelacionadas())
                .contains("/api/v1/processual/recursal/notification/preferences/fine", "/api/v1/processual/recursal/notification/federated-delivery");
    }

    @Test
    void deveEndurecerEntregaFederadaSoberanaSemPipelineParalelo() {
        RecursalNotificationFederatedDeliveryResponse response = service.federatedDelivery(baseRequest(true, false, true, true, true, false, true, true, "SECRETARIA"));

        assertThat(response.titulo()).isEqualTo("ENTREGA_FEDERADA_SOBERANA");
        assertThat(response.entregaExternaSoberana()).isTrue();
        assertThat(response.politicaFederada()).isEqualTo("POLITICA_FEDERADA_SOBERANA");
        assertThat(response.politicasAplicadas()).contains("ENDURECER_ENTREGA_EXTERNA_SOBERANA", "FEDERAR_ENTREGA_INSTITUCIONAL");
    }

    private RecursalNotificationPreferencePolicyRequest baseRequest(boolean sigiloso,
                                                                    boolean mobileAtivo,
                                                                    boolean canalPush,
                                                                    boolean canalInbox,
                                                                    boolean canalEmail,
                                                                    boolean canalSms,
                                                                    boolean federacaoInstitucionalAtiva,
                                                                    boolean dominioSoberanoExterno,
                                                                    String perfilDestino) {
        return new RecursalNotificationPreferencePolicyRequest(
                new RecursalNotificationGovernanceRequest(
                        baseAutomationRequest(),
                        "0001234-56.2026.8.13.0001",
                        "USR-001",
                        perfilDestino,
                        sigiloso,
                        false,
                        mobileAtivo,
                        true,
                        8,
                        "TOKEN-ABC"
                ),
                canalPush,
                canalInbox,
                true,
                canalEmail,
                canalSms,
                federacaoInstitucionalAtiva,
                dominioSoberanoExterno,
                dominioSoberanoExterno ? "dominio.soberano.externo" : "pjb.interno"
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
