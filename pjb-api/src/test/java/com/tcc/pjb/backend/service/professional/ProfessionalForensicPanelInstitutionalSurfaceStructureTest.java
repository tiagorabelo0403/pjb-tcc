package com.tcc.pjb.backend.service.professional;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProfessionalForensicPanelInstitutionalSurfaceStructureTest {

    private static final Path CONTROLLER = Path.of("src/main/java/com/tcc/pjb/backend/controller/professional/ProfessionalForensicPanelController.java");
    private static final Path SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/service/professional/ProfessionalForensicPanelService.java");
    private static final Path ROUTES = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/consultapublica/ConsultaPublicaWorkspaceRoutesDto.java");

    @Test
    void mustExposeInstitutionalOverviewAndClient360Surfaces() throws Exception {
        String controller = Files.readString(CONTROLLER, StandardCharsets.UTF_8);
        String service = Files.readString(SERVICE, StandardCharsets.UTF_8);
        assertTrue(controller.contains("@GetMapping(\"/institutional-overview\")"));
        assertTrue(controller.contains("@GetMapping(\"/client-360\")"));
        assertTrue(service.contains("public ProfessionalForensicInstitutionalOverviewResponse institutionalOverview("));
        assertTrue(service.contains("public ProfessionalForensicClient360Response client360("));
        assertTrue(service.contains("buildInstitutionalModules("));
        assertTrue(service.contains("client360Route("));
    }

    @Test
    void mustConnectPublicWorkspaceRoutesToProfessionalPanel() throws Exception {
        String routes = Files.readString(ROUTES, StandardCharsets.UTF_8);
        assertTrue(routes.contains("String professionalPanelWorkspace"));
        assertTrue(routes.contains("String professionalPanelSearch"));
        assertTrue(routes.contains("String professionalInstitutionalOverview"));
        assertTrue(routes.contains("String professionalClient360"));
        assertTrue(routes.contains("String professionalProcessDetail"));
    }
}
