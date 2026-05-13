package com.tcc.pjb.backend.controller.processual.recursal.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyRequest;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.notification.RecursalNotificationPreferenceFederationService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalOperationalSurfaceProjectionSupport;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecursalNotificationPreferenceFederationControllerIT {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        RecursalAutomationService automationService = new RecursalAutomationService();
        RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
        RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
        RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
        RecursalIntelligenceSurfaceService intelligenceSurfaceService = new RecursalIntelligenceSurfaceService(projectionSupport);
        RecursalNotificationPreferenceFederationService service = new RecursalNotificationPreferenceFederationService(intelligenceSurfaceService);
        mockMvc = MockMvcBuilders.standaloneSetup(new RecursalNotificationPreferenceFederationController(service)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deveProjetarPreferenciasFinasViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.NOTIFICATION_PREFERENCES_FINE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest(false, true, true, true, false, false, true, false, "ADVOGADO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("PREFERENCIAS_FINAS_RECURSAIS"))
                .andExpect(jsonPath("$.canalPreferencial").value("PUSH_GOVERNADO"))
                .andExpect(jsonPath("$.politicaFederada").value("POLITICA_FEDERADA_INSTITUCIONAL"));
    }

    @Test
    void deveProjetarEntregaFederadaSoberanaViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.NOTIFICATION_FEDERATED_DELIVERY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest(true, false, true, true, true, false, true, true, "SECRETARIA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("ENTREGA_FEDERADA_SOBERANA"))
                .andExpect(jsonPath("$.entregaExternaSoberana").value(true))
                .andExpect(jsonPath("$.politicaFederada").value("POLITICA_FEDERADA_SOBERANA"));
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
