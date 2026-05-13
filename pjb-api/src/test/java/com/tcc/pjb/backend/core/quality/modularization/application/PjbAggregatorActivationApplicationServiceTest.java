package com.tcc.pjb.backend.core.quality.modularization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbAggregatorActivationApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void snapshot_deveIndicarBloqueioQuandoPomRaizNaoUsaPackagingPom() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project><packaging>jar</packaging></project>");
        Files.writeString(tempDir.resolve("pom.phase1-aggregator.xml"), "<modules><module>pjb-core</module><module>pjb-api</module></modules>");
        Files.createDirectories(tempDir.resolve("pjb-core/src/main/java"));
        Files.createDirectories(tempDir.resolve("pjb-api/src/main/java"));
        Files.writeString(tempDir.resolve("pjb-core/pom.xml"), "<project/>");
        Files.writeString(tempDir.resolve("pjb-api/pom.xml"), "<project/>");
        var service = new PjbAggregatorActivationApplicationService(mock(AuditLedgerService.class), tempDir);

        var snapshot = service.snapshot();

        assertThat(snapshot.phaseOneAggregatorFilePresent()).isTrue();
        assertThat(snapshot.activationReady()).isFalse();
        assertThat(snapshot.blockers()).anyMatch(item -> item.contains("packaging pom"));
    }

    @Test
    void moduleLinks_deveLerAgregadorGerado() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project><packaging>jar</packaging></project>");
        Files.writeString(tempDir.resolve("pom.phase1-aggregator.xml"), "<modules><module>pjb-core</module><module>pjb-api</module></modules>");
        Files.createDirectories(tempDir.resolve("pjb-core/src/main/java"));
        Files.createDirectories(tempDir.resolve("pjb-api/src/main/java"));
        Files.writeString(tempDir.resolve("pjb-core/pom.xml"), "<project/>");
        Files.writeString(tempDir.resolve("pjb-api/pom.xml"), "<project/>");
        var service = new PjbAggregatorActivationApplicationService(mock(AuditLedgerService.class), tempDir);

        var links = service.moduleLinks();

        assertThat(links).hasSize(2);
        assertThat(links.getFirst().listedInAggregatorFile()).isTrue();
        assertThat(links.getFirst().listedInRootPom()).isFalse();
    }

    @Test
    void pomPatch_deveApontarArquivoGeradoDaFaseUm() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project><packaging>jar</packaging></project>");
        Files.writeString(tempDir.resolve("pom.phase1-aggregator.xml"), "<project/>");
        var service = new PjbAggregatorActivationApplicationService(mock(AuditLedgerService.class), tempDir);

        var patch = service.pomPatch();

        assertThat(patch.generatedFilePresent()).isTrue();
        assertThat(patch.targetFile()).isEqualTo("pom.phase1-aggregator.xml");
        assertThat(patch.patchLines()).anyMatch(line -> line.contains("<module>pjb-core</module>"));
    }
}
