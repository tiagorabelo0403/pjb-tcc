package com.tcc.pjb.backend.service.processual.recursal.automation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalAutomationPackageStructureRefinementArchitectureTest {

    @Test
    void deveManterControllersEServicesDeAutomationOrganizados() {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/automation/RecursalAutomationService.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/automation/RecursalAutomationPlaybookService.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/automation/RecursalAutomationController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/automation/RecursalAutomationPlaybookController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/workspace/RecursalAutomationWorkspaceController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalAutomationWorkspaceService.java"))).isTrue();
    }
}
