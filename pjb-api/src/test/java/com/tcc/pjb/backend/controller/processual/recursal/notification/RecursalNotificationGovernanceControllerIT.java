package com.tcc.pjb.backend.controller.processual.recursal.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.notification.RecursalNotificationGovernanceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalOperationalSurfaceProjectionSupport;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecursalNotificationGovernanceControllerIT {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        RecursalAutomationService automationService = new RecursalAutomationService();
        RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
        RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
        RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
        RecursalIntelligenceSurfaceService intelligenceSurfaceService = new RecursalIntelligenceSurfaceService(projectionSupport);
        RecursalNotificationGovernanceService governanceService = new RecursalNotificationGovernanceService(intelligenceSurfaceService);

        mockMvc = MockMvcBuilders.standaloneSetup(new RecursalNotificationGovernanceController(governanceService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deveProjetarPreviewMobileGovernadoViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.NOTIFICATION_MOBILE_PREVIEW)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest(false, true, true, true, 5, "ADVOGADO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("PREVIEW_MOBILE_PENDENCIAS_RECURSAIS"))
                .andExpect(jsonPath("$.canalPrioritario").value("PUSH_GOVERNADO"))
                .andExpect(jsonPath("$.rotasRelacionadas[1]").value("/api/v1/processual/recursal/analytics/notifica-pendencias"));
    }

    @Test
    void deveProjetarGovernancaNotificacionalViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.NOTIFICATION_GOVERNANCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest(true, false, false, true, 20, "SECRETARIA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("GOVERNANCA_NOTIFICACIONAL_RECURSAL"))
                .andExpect(jsonPath("$.schedulerParaleloPermitido").value(false))
                .andExpect(jsonPath("$.executorParaleloPermitido").value(false));
    }

    @Test
    void deveProjetarCienciaNotificacionalViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.NOTIFICATION_SCIENCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest(false, false, true, true, 12, "MP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("CIENCIA_NOTIFICACIONAL_RECURSAL"))
                .andExpect(jsonPath("$.statusCiencia").value("CIENCIA_REGISTRADA"))
                .andExpect(jsonPath("$.rotaTrackingCiencia").value("/api/v1/notificacoes/track/{token}/ciencia"));
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
