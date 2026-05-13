package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.controller.processual.recursal.automation.RecursalAutomationController;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationResponse;
import com.tcc.pjb.backend.service.processual.recursal.automation.RecursalAutomationService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalAutomationStructureRefinementArchitectureTest {

    @Test
    void automationDeveViverEmSubpacoteDedicado() {
        assertThat(RecursalAutomationService.class.getPackageName()).endsWith(".automation");
        assertThat(RecursalAutomationResponse.class.getPackageName()).endsWith(".automation");
        assertThat(RecursalAutomationController.class.getPackageName()).endsWith(".automation");
    }

    @Test
    void rotaAutomaticaRecursalDeveSerCentralizada() {
        assertThat(RecursalRoutes.AUTOMATION_ADVISE).isEqualTo("/automation/advise");
    }

    @Test
    void arquivosRecursaisAutomaticosDevemExistirNosSubpacotesCertos() throws Exception {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/automation/RecursalAutomationService.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/automation/RecursalAutomationController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/automation/RecursalAutomationResponse.java"))).isTrue();
    }
}
