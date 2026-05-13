package com.tcc.pjb.backend.service.processual.recursal.surface;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalOperationalSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationPlaybookService;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalAutomationWorkspaceService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecursalOperationalSurfaceServiceTest {

    private final RecursalAutomationService automationService = new RecursalAutomationService();
    private final RecursalAutomationPlaybookService playbookService = new RecursalAutomationPlaybookService(automationService);
    private final RecursalAutomationWorkspaceService workspaceService = new RecursalAutomationWorkspaceService(automationService, playbookService);
    private final RecursalOperationalSurfaceProjectionSupport projectionSupport = new RecursalOperationalSurfaceProjectionSupport(workspaceService);
    private final RecursalOperationalSurfaceService operationalSurfaceService = new RecursalOperationalSurfaceService(projectionSupport);

    @Test
    void deveMaterializarSurfaceOperacionalComSecoesEspecializadas() {
        RecursalOperationalSurfaceResponse response = operationalSurfaceService.buildOperationalSurface(new RecursalAutomationRequest(
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
        ));

        assertThat(response.rotaPrioritaria()).isEqualTo("APELACAO");
        assertThat(response.secoes()).extracting(section -> section.codigo())
                .containsExactly(
                        "SURFACE_ADVOGADO_RECURSAL",
                        "SURFACE_INSTITUCIONAL_RECURSAL",
                        "SURFACE_DOCUMENTAL_RECURSAL",
                        "SURFACE_INTELIGENCIA_RECURSAL"
                );
        assertThat(response.faltantes()).extracting(gap -> gap.codigo())
                .contains(
                        "SPECIALIZED_CONTRACTS_AND_ITS",
                        "DOCUMENT_VIEWER_ASSINATURA_AUTENTICIDADE",
                        "MOBILE_PUSH_GOVERNANCE",
                        "GLOBAL_COMPILE_RECOVERY"
                )
                .doesNotContain("HTTP_SURFACES_REALIZADAS", "CONTRACTS_AND_ITS");
    }

    @Test
    void devePreservarSurfaceBloqueadaQuandoPoderRecorrerEstiverBloqueado() {
        RecursalOperationalSurfaceResponse response = operationalSurfaceService.buildOperationalSurface(new RecursalAutomationRequest(
                "SENTENCA",
                "REFORMAR",
                false,
                false,
                false,
                false,
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
                false,
                Set.of()
        ));

        assertThat(response.bloqueado()).isTrue();
        assertThat(response.motivoBloqueio()).isNotBlank();
        assertThat(response.faltantes()).extracting(gap -> gap.codigo())
                .contains("SPECIALIZED_CONTRACTS_AND_ITS", "GLOBAL_COMPILE_RECOVERY")
                .doesNotContain("MOBILE_PUSH_GOVERNANCE");
    }
}
