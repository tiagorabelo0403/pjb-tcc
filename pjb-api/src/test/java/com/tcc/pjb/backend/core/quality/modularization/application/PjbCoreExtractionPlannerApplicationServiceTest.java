package com.tcc.pjb.backend.core.quality.modularization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbCoreExtractionPlannerApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void snapshot_deveLerCandidatosEDependencias() throws Exception {
        prepareProject(true);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbCoreExtractionPlannerApplicationService service = new PjbCoreExtractionPlannerApplicationService(auditLedgerService, tempDir);

        var snapshot = service.snapshot();

        assertThat(snapshot.aggregatorPomPresent()).isTrue();
        assertThat(snapshot.candidatePackageCount()).isGreaterThanOrEqualTo(2);
        assertThat(snapshot.dependencyIssueCount()).isGreaterThanOrEqualTo(1);
        assertThat(snapshot.recommendedSteps()).isNotEmpty();
    }

    @Test
    void pomPreview_deveSugerirModulosDaFaseUm() throws Exception {
        prepareProject(false);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbCoreExtractionPlannerApplicationService service = new PjbCoreExtractionPlannerApplicationService(auditLedgerService, tempDir);

        var preview = service.pomPreview();

        assertThat(preview.aggregatorPomRequired()).isTrue();
        assertThat(preview.suggestedModules()).contains("pjb-core", "pjb-api");
        assertThat(preview.previewLines()).anyMatch(line -> line.contains("<module>pjb-core</module>"));
    }

    @Test
    void movePlan_devePriorizarPacotesDeBaixoRisco() throws Exception {
        prepareProject(true);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbCoreExtractionPlannerApplicationService service = new PjbCoreExtractionPlannerApplicationService(auditLedgerService, tempDir);

        var plan = service.movePlan();

        assertThat(plan).hasSize(2);
        assertThat(plan.get(0).phase()).isEqualTo("Fase 1");
        assertThat(plan.get(0).candidatePackages()).isNotEmpty();
    }

    private void prepareProject(boolean withModules) throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/audit"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/prazos"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/platform/runtime"));
        String pom = withModules
                ? "<project><modules><module>pjb-core</module><module>pjb-api</module></modules></project>"
                : "<project></project>";
        Files.writeString(tempDir.resolve("pom.xml"), pom, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/audit/AuditSupport.java"),
                "package com.tcc.pjb.backend.core.audit; public class AuditSupport {}",
                StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/platform/runtime/RuntimeSizing.java"),
                "package com.tcc.pjb.backend.platform.runtime; public class RuntimeSizing {}",
                StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/prazos/PrazoEngineFacade.java"),
                "package com.tcc.pjb.backend.core.prazos; import com.tcc.pjb.backend.service.LegacyService; public class PrazoEngineFacade {}",
                StandardCharsets.UTF_8);
    }
}
