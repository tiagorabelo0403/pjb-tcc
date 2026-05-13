package com.tcc.pjb.backend.core.quality.modularization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbModuleScaffoldApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void snapshot_deveLerScaffoldFisicoDosModulos() throws Exception {
        prepareProject(true);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbModuleScaffoldApplicationService service = new PjbModuleScaffoldApplicationService(auditLedgerService, tempDir);

        var snapshot = service.snapshot();

        assertThat(snapshot.scaffoldPresent()).isTrue();
        assertThat(snapshot.aggregatorLinked()).isTrue();
        assertThat(snapshot.modulePomCount()).isEqualTo(2);
        assertThat(snapshot.scaffoldDirectoryCount()).isEqualTo(4);
    }

    @Test
    void modulePoms_deveExporArtifactIdEPackaging() throws Exception {
        prepareProject(false);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbModuleScaffoldApplicationService service = new PjbModuleScaffoldApplicationService(auditLedgerService, tempDir);

        var poms = service.modulePoms();

        assertThat(poms).hasSize(2);
        assertThat(poms.get(0).artifactId()).isEqualTo("pjb-core");
        assertThat(poms.get(1).packaging()).isEqualTo("jar");
    }

    @Test
    void buildOrder_deveExporSequenciaCoreDepoisApi() {
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbModuleScaffoldApplicationService service = new PjbModuleScaffoldApplicationService(auditLedgerService, tempDir);

        var buildOrder = service.buildOrder();

        assertThat(buildOrder).hasSize(2);
        assertThat(buildOrder.get(0).moduleName()).isEqualTo("pjb-core");
        assertThat(buildOrder.get(1).moduleName()).isEqualTo("pjb-api");
    }

    private void prepareProject(boolean withAggregatorLinks) throws Exception {
        Files.createDirectories(tempDir.resolve("pjb-core/src/main/java"));
        Files.createDirectories(tempDir.resolve("pjb-core/src/test/java"));
        Files.createDirectories(tempDir.resolve("pjb-api/src/main/java"));
        Files.createDirectories(tempDir.resolve("pjb-api/src/test/java"));
        String rootPom = withAggregatorLinks
                ? "<project><modules><module>pjb-core</module><module>pjb-api</module></modules></project>"
                : "<project></project>";
        Files.writeString(tempDir.resolve("pom.xml"), rootPom, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("pjb-core/pom.xml"),
                "<project><parent><artifactId>spring-boot-starter-parent</artifactId></parent><artifactId>pjb-core</artifactId><packaging>jar</packaging></project>",
                StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("pjb-api/pom.xml"),
                "<project><parent><artifactId>spring-boot-starter-parent</artifactId></parent><artifactId>pjb-api</artifactId><packaging>jar</packaging></project>",
                StandardCharsets.UTF_8);
    }
}
