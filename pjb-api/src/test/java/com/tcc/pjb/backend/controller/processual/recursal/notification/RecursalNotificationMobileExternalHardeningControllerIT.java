package com.tcc.pjb.backend.controller.processual.recursal.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobileHardeningRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyRequest;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.notification.RecursalNotificationMobileExternalHardeningService;
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

class RecursalNotificationMobileExternalHardeningControllerIT {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        RecursalAutomationService automationService = new RecursalAutomationService();
        RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
        RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
        RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
        RecursalIntelligenceSurfaceService intelligenceSurfaceService = new RecursalIntelligenceSurfaceService(projectionSupport);
        RecursalNotificationPreferenceFederationService preferenceFederationService = new RecursalNotificationPreferenceFederationService(intelligenceSurfaceService);
        RecursalNotificationMobileExternalHardeningService service = new RecursalNotificationMobileExternalHardeningService(preferenceFederationService);
        mockMvc = MockMvcBuilders.standaloneSetup(new RecursalNotificationMobileExternalHardeningController(service)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deveProjetarPosturaMobileViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.NOTIFICATION_MOBILE_POSTURE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("POSTURA_MOBILE_SOBERANA"))
                .andExpect(jsonPath("$.posturaStatus").value("POSTURA_APROVADA"));
    }

    @Test
    void deveProjetarEntregaMobileEndurecidaViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.NOTIFICATION_MOBILE_EXTERNAL_HARDENING)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("ENDURECIMENTO_MOBILE_EXTERNO_SOBERANO"))
                .andExpect(jsonPath("$.statusEntrega").value("ENTREGA_MOBILE_BLOQUEADA"));
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
