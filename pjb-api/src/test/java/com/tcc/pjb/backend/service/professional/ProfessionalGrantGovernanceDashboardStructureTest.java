package com.tcc.pjb.backend.service.professional;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProfessionalGrantGovernanceDashboardStructureTest {

    private static final Path CONTROLLER = Path.of("src/main/java/com/tcc/pjb/backend/controller/professional/ProfessionalInstitutionalAccessGrantAdminController.java");
    private static final Path SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/service/professional/ProfessionalInstitutionalAccessGrantAdminService.java");
    private static final Path REPOSITORY = Path.of("src/main/java/com/tcc/pjb/backend/model/repository/professional/ProfessionalInstitutionalAccessGrantRepository.java");
    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V214__professional_access_grant_governance_dashboard.sql");

    @Test
    void governanceDashboardAndBatchOperationsMustExist() throws Exception {
        String controller = Files.readString(CONTROLLER);
        String service = Files.readString(SERVICE);
        String repository = Files.readString(REPOSITORY);
        String migration = Files.readString(MIGRATION);

        assertTrue(controller.contains("@GetMapping(\"/governance-dashboard\")"));
        assertTrue(controller.contains("@PostMapping(\"/batch-requests\")"));
        assertTrue(controller.contains("@PostMapping(\"/batch-approve\")"));
        assertTrue(controller.contains("@PostMapping(\"/batch-revoke\")"));

        assertTrue(service.contains("public ProfessionalGrantGovernanceWorkspaceResponse governanceDashboard("));
        assertTrue(service.contains("public ProfessionalGrantBatchOperationResponse issueBatch("));
        assertTrue(service.contains("public ProfessionalGrantBatchOperationResponse approveBatch("));
        assertTrue(service.contains("public ProfessionalGrantBatchOperationResponse revokeBatch("));
        assertTrue(service.contains("matchesGovernanceFilters("));

        assertTrue(repository.contains("findTop500ByOrderByIdDesc"));
        assertTrue(migration.contains("idx_prof_access_grant_status_actor_window"));
        assertTrue(migration.contains("idx_prof_access_grant_gov_anchor"));
    }
}
