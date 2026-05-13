package com.tcc.pjb.backend.service.processual.recursal.documental;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentAuthenticityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentSignatureEvidenceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentViewerResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentalArtifactRequest;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalDocumentalSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalOperationalSurfaceProjectionSupport;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalDocumentalSovereignSuiteServiceTest {

    private final RecursalAutomationService automationService = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
    private final RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
    private final RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
    private final RecursalDocumentalSurfaceService documentalSurfaceService = new RecursalDocumentalSurfaceService(projectionSupport);
    private final RecursalDocumentalSovereignSuiteService sovereignSuiteService = new RecursalDocumentalSovereignSuiteService(documentalSurfaceService);

    @Test
    void deveProjetarViewerSoberanoComHashERotasRelacionadas() {
        RecursalDocumentViewerResponse response = sovereignSuiteService.viewer(baseArtifact(false, true, true, false));

        assertThat(response.eixo()).isEqualTo("SUITE_DOCUMENTAL_SOBERANA");
        assertThat(response.titulo()).isEqualTo("VISUALIZADOR_DOCUMENTAL_SOBERANO");
        assertThat(response.algoritmoHash()).isEqualTo("SHA-256");
        assertThat(response.hashReferencia()).hasSize(64);
        assertThat(response.rotasRelacionadas())
                .contains("/api/v1/processual/recursal/document-viewer", "/api/v1/processual/recursal/document-authenticity", "/api/v1/processual/recursal/document-signature-evidence");
    }

    @Test
    void deveProjetarAutenticidadeComEnvelopeProvaEConferenciaPublica() {
        RecursalDocumentAuthenticityResponse response = sovereignSuiteService.authenticity(baseArtifact(false, true, true, false));

        assertThat(response.titulo()).isEqualTo("AUTENTICIDADE_DOCUMENTAL_SOBERANA");
        assertThat(response.envelopeProva()).startsWith("ENV-");
        assertThat(response.rotaConferenciaPublica()).isEqualTo("/api/v1/certidoes/autenticidade");
        assertThat(response.modoValidacao()).isEqualTo("VALIDACAO_PUBLICA_CONTROLADA");
        assertThat(response.statusAutenticidade()).isEqualTo("VALIDO");
    }

    @Test
    void deveProjetarAssinaturaSoberanaSigilosaComCadeiaControlada() {
        RecursalDocumentSignatureEvidenceResponse response = sovereignSuiteService.signature(baseArtifact(true, true, false, true));

        assertThat(response.titulo()).isEqualTo("EVIDENCIA_ASSINATURA_DOCUMENTAL_SOBERANA");
        assertThat(response.modoAssinatura()).isEqualTo("ASSINATURA_CONTROLADA_PJB");
        assertThat(response.statusAssinatura()).isEqualTo("SIGILO");
        assertThat(response.cadeiaCertificados()).contains("PJB-CONTROLADA", "RFC_3161", "LTV_PDF");
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
