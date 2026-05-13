package com.tcc.pjb.backend.service.professional;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProfessionalGrantAdminSurfaceStructureTest {

    private static final Path CONTROLLER = Path.of("src/main/java/com/tcc/pjb/backend/controller/professional/ProfessionalInstitutionalAccessGrantAdminController.java");
    private static final Path SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/service/professional/ProfessionalInstitutionalAccessGrantAdminService.java");
    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V213__professional_access_grant_governance.sql");

    @Test
    void controllerAndServiceMustExposeGrantGovernanceFlow() throws Exception {
        String controller = Files.readString(CONTROLLER);
        String service = Files.readString(SERVICE);
        String migration = Files.readString(MIGRATION);

        assertTrue(controller.contains("@RequestMapping(\"/api/v1/professional/access-grants\")"));
        assertTrue(controller.contains("@PostMapping(\"/requests\")"));
        assertTrue(controller.contains("@PostMapping(\"/{grantId}/approve\")"));
        assertTrue(controller.contains("@PostMapping(\"/{grantId}/reject\")"));
        assertTrue(controller.contains("@PostMapping(\"/{grantId}/revoke\")"));
        assertTrue(controller.contains("@GetMapping(\"/processos/{numero}/timeline\")"));

        assertTrue(service.contains("public ProfessionalGrantDetailResponse issue("));
        assertTrue(service.contains("public ProfessionalGrantDetailResponse approve("));
        assertTrue(service.contains("public ProfessionalGrantDetailResponse reject("));
        assertTrue(service.contains("public ProfessionalGrantDetailResponse revoke("));
        assertTrue(service.contains("public ProfessionalGrantProcessTimelineResponse processTimeline("));

        assertTrue(migration.contains("tb_professional_access_grant_event"));
        assertTrue(migration.contains("approval_status"));
    }
}
