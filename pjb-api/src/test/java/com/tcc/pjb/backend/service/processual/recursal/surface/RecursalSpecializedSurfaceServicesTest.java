package com.tcc.pjb.backend.service.processual.recursal.surface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalSpecializedSurfaceServicesTest {

    private final RecursalAutomationService automationService = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
    private final RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
    private final RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
    private final RecursalAttorneySurfaceService attorneySurfaceService = new RecursalAttorneySurfaceService(projectionSupport);
    private final RecursalDocumentalSurfaceService documentalSurfaceService = new RecursalDocumentalSurfaceService(projectionSupport);

    @Test
    void deveProjetarSurfaceEspecializadaDoAdvogado() {
        RecursalSpecializedSurfaceResponse response = attorneySurfaceService.buildAttorneySurface(baseRequest());

        assertThat(response.eixo()).isEqualTo("SURFACE_ADVOGADO_RECURSAL");
        assertThat(response.rotaBase()).isEqualTo("/surfaces/attorney");
        assertThat(response.trilhas()).contains(
                "PAINEL_ADVOGADO_RECURSAL_COMPLETO",
                "PETICIONAMENTO_LOTE_ASSINATURA_RECURSAL"
        );
        assertThat(response.gaps()).extracting(gap -> gap.codigo())
                .containsExactly("SPECIALIZED_CONTRACTS_AND_ITS");
    }

    @Test
    void deveProjetarSurfaceEspecializadaDocumentalComGapDocumentalExplicito() {
        RecursalSpecializedSurfaceResponse response = documentalSurfaceService.buildDocumentalSurface(baseRequest());

        assertThat(response.eixo()).isEqualTo("SURFACE_DOCUMENTAL_RECURSAL");
        assertThat(response.rotaBase()).isEqualTo("/surfaces/documental");
        assertThat(response.trilhas()).contains(
                "AUTOS_DIGITAIS_RECURSAIS_DETALHADOS",
                "WIZARD_DISTRIBUICAO_ASSISTIDA_IA"
        );
        assertThat(response.gaps()).extracting(gap -> gap.codigo())
                .contains("SPECIALIZED_CONTRACTS_AND_ITS", "DOCUMENT_VIEWER_ASSINATURA_AUTENTICIDADE");
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
                Set.of()
        );
    }
}
