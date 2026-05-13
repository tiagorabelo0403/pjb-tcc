package com.tcc.pjb.backend.service.professional;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProfessionalGrantOperationalQueueStructureTest {

    private static final Path CONTROLLER = Path.of("src/main/java/com/tcc/pjb/backend/controller/professional/ProfessionalInstitutionalAccessGrantAdminController.java");
    private static final Path SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/service/professional/ProfessionalInstitutionalAccessGrantAdminService.java");
    private static final Path TEMPLATE_ENTITY = Path.of("src/main/java/com/tcc/pjb/backend/model/entity/professional/ProfessionalAccessGrantTemplate.java");
    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V215__professional_access_grant_operational_templates.sql");

    @Test
    void operationalDashboardAndTemplatesMustExist() throws Exception {
        String controller = Files.readString(CONTROLLER);
        String service = Files.readString(SERVICE);
        String templateEntity = Files.readString(TEMPLATE_ENTITY);
        String migration = Files.readString(MIGRATION);

        assertTrue(controller.contains("@GetMapping(\"/operational-dashboard\")"));
        assertTrue(controller.contains("@GetMapping(\"/templates\")"));
        assertTrue(controller.contains("@PostMapping(\"/template-batch-requests\")"));

        assertTrue(service.contains("public ProfessionalGrantOperationalQueueResponse operationalDashboard("));
        assertTrue(service.contains("public ProfessionalGrantTemplateCatalogResponse templates()"));
        assertTrue(service.contains("public ProfessionalGrantBatchOperationResponse issueBatchFromTemplate("));
        assertTrue(service.contains("suggestedTemplates("));

        assertTrue(templateEntity.contains("class ProfessionalAccessGrantTemplate"));
        assertTrue(migration.contains("tb_professional_access_grant_template"));
        assertTrue(migration.contains("MAG_DELEGACAO_GABINETE"));
    }
}
