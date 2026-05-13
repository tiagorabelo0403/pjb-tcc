package com.tcc.pjb.backend.core.quality.apisurface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalGovernanceSecurityNotificationSurfaceDisciplineTest {

    @Test
    void controllersGovernancaInstitucionalSegurancaENotificacaoNaoDevemExporDominioNemDtosInline() throws IOException {
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/affiliation/NationalCommunicationInstitutionalAffiliationController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalGovernanceHardeningController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/security/NationalCommunicationInstitutionalSecurityGovernanceController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/security/SecurityContextController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/notification/IntimacaoMulticanalController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/notification/NotificationTrackingController.java");
    }

    private static void assertControllerClean(String path) throws IOException {
        String source = Files.readString(Path.of(path));
        assertFalse(source.contains("record "));
        assertFalse(source.contains("Map<String, Object>"));
        assertFalse(source.contains("core.comunicacao.institucional.") && !source.contains("core.comunicacao.institucional.InstitutionalApiRoutes"));
        assertFalse(source.contains("IntimacaoMulticanalService.DispatchPlanResponse"));
    }
}
