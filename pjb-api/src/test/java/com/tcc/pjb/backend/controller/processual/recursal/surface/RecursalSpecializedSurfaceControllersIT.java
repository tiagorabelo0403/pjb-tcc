package com.tcc.pjb.backend.controller.processual.recursal.surface;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalAttorneySurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalDocumentalSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalInstitutionalSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalOperationalSurfaceProjectionSupport;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecursalSpecializedSurfaceControllersIT {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        RecursalAutomationService automationService = new RecursalAutomationService();
        RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
        RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
        RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new RecursalAttorneySurfaceController(new RecursalAttorneySurfaceService(projectionSupport)),
                new RecursalInstitutionalSurfaceController(new RecursalInstitutionalSurfaceService(projectionSupport)),
                new RecursalDocumentalSurfaceController(new RecursalDocumentalSurfaceService(projectionSupport)),
                new RecursalIntelligenceSurfaceController(new RecursalIntelligenceSurfaceService(projectionSupport))
        ).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deveProjetarSurfaceEspecializadaDoAdvogadoViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.SURFACES_ATTORNEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eixo").value("SURFACE_ADVOGADO_RECURSAL"))
                .andExpect(jsonPath("$.rotaBase").value("/surfaces/attorney"))
                .andExpect(jsonPath("$.trilhas[0]").exists())
                .andExpect(jsonPath("$.gaps[0].codigo").value("SPECIALIZED_CONTRACTS_AND_ITS"));
    }

    @Test
    void deveProjetarSurfaceEspecializadaInstitucionalViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.SURFACES_INSTITUTIONAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eixo").value("SURFACE_INSTITUCIONAL_RECURSAL"))
                .andExpect(jsonPath("$.rotaBase").value("/surfaces/institutional"))
                .andExpect(jsonPath("$.trilhas").isArray())
                .andExpect(jsonPath("$.gaps[0].codigo").value("SPECIALIZED_CONTRACTS_AND_ITS"));
    }

    @Test
    void deveProjetarSurfaceEspecializadaDocumentalViaHttpComGapDocumental() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.SURFACES_DOCUMENTAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eixo").value("SURFACE_DOCUMENTAL_RECURSAL"))
                .andExpect(jsonPath("$.rotaBase").value("/surfaces/documental"))
                .andExpect(jsonPath("$.gaps[0].codigo").value("SPECIALIZED_CONTRACTS_AND_ITS"))
                .andExpect(jsonPath("$.gaps[1].codigo").value("DOCUMENT_VIEWER_ASSINATURA_AUTENTICIDADE"));
    }

    @Test
    void deveProjetarSurfaceEspecializadaDeInteligenciaViaHttpComGapMobile() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.SURFACES_INTELLIGENCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eixo").value("SURFACE_INTELIGENCIA_RECURSAL"))
                .andExpect(jsonPath("$.rotaBase").value("/surfaces/intelligence"))
                .andExpect(jsonPath("$.gaps[0].codigo").value("SPECIALIZED_CONTRACTS_AND_ITS"))
                .andExpect(jsonPath("$.gaps[1].codigo").value("MOBILE_PUSH_GOVERNANCE"));
    }

    private RecursalAutomationRequest baseRequest() {
        return new RecursalAutomationRequest(
                "SENTENCA",
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
