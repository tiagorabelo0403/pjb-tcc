package com.tcc.pjb.backend.controller.processual.recursal.documental;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentalArtifactRequest;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.documental.RecursalDocumentalSovereignSuiteService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalDocumentalSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalOperationalSurfaceProjectionSupport;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecursalDocumentalSovereignSuiteControllerIT {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        RecursalAutomationService automationService = new RecursalAutomationService();
        RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
        RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
        RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
        RecursalDocumentalSurfaceService documentalSurfaceService = new RecursalDocumentalSurfaceService(projectionSupport);
        RecursalDocumentalSovereignSuiteService sovereignSuiteService = new RecursalDocumentalSovereignSuiteService(documentalSurfaceService);

        mockMvc = MockMvcBuilders.standaloneSetup(new RecursalDocumentalSovereignSuiteController(sovereignSuiteService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deveProjetarViewerDocumentalSoberanoViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.DOCUMENT_VIEWER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseArtifact(false, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("VISUALIZADOR_DOCUMENTAL_SOBERANO"))
                .andExpect(jsonPath("$.algoritmoHash").value("SHA-256"))
                .andExpect(jsonPath("$.rotasRelacionadas[2]").value("/api/v1/processual/recursal/document-signature-evidence"));
    }

    @Test
    void deveProjetarAutenticidadeDocumentalSoberanaViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.DOCUMENT_AUTHENTICITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseArtifact(false, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("AUTENTICIDADE_DOCUMENTAL_SOBERANA"))
                .andExpect(jsonPath("$.statusAutenticidade").value("VALIDO"))
                .andExpect(jsonPath("$.rotaConferenciaPublica").value("/api/v1/certidoes/autenticidade"));
    }

    @Test
    void deveProjetarEvidenciaDeAssinaturaSigilosaViaHttp() throws Exception {
        mockMvc.perform(post(RecursalRoutes.BASE + RecursalRoutes.DOCUMENT_SIGNATURE_EVIDENCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(baseArtifact(true, true, false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("EVIDENCIA_ASSINATURA_DOCUMENTAL_SOBERANA"))
                .andExpect(jsonPath("$.statusAssinatura").value("SIGILO"))
                .andExpect(jsonPath("$.cadeiaCertificados[0]").value("PJB-CONTROLADA"));
    }

    private RecursalDocumentalArtifactRequest baseArtifact(boolean sigiloso,
                                                           boolean certificadoDisponivel,
                                                           boolean assinaturaQualificada,
                                                           boolean midiaAudiovisual) {
        return new RecursalDocumentalArtifactRequest(
                baseRequest(),
                "0001234-56.2026.8.13.0001",
                "ARTEFATO-001",
                "ACORDAO",
                sigiloso,
                certificadoDisponivel,
                assinaturaQualificada,
                midiaAudiovisual
        );
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
