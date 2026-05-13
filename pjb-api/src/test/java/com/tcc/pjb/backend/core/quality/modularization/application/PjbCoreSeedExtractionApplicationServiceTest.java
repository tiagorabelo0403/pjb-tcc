package com.tcc.pjb.backend.core.quality.modularization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbCoreSeedExtractionApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void snapshot_deveIndicarBloqueioQuandoEspelhoNaoCobrirTodasAsClasses() throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/modularity");
        Path moduleRoot = tempDir.resolve("pjb-core/src/main/java/com/tcc/pjb/backend/core/modularity");
        Files.createDirectories(sourceRoot);
        Files.createDirectories(moduleRoot);
        Files.writeString(sourceRoot.resolve("PjbModuleId.java"), "class A {}\n");
        Files.writeString(sourceRoot.resolve("PjbModulePort.java"), "interface B {}\n");
        Files.writeString(moduleRoot.resolve("PjbModuleId.java"), "class A {}\n");
        var service = new PjbCoreSeedExtractionApplicationService(mock(AuditLedgerService.class), tempDir);

        var snapshot = service.snapshot();

        assertThat(snapshot.sourcePackagePresent()).isTrue();
        assertThat(snapshot.moduleMirrorPresent()).isTrue();
        assertThat(snapshot.parityReady()).isFalse();
        assertThat(snapshot.blockers()).anyMatch(item -> item.contains("mesma quantidade de classes"));
    }

    @Test
    void parity_deveMarcarClasseComoAlinhadaQuandoHashesCoincidem() throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/modularity");
        Path moduleRoot = tempDir.resolve("pjb-core/src/main/java/com/tcc/pjb/backend/core/modularity");
        Files.createDirectories(sourceRoot);
        Files.createDirectories(moduleRoot);
        Files.writeString(sourceRoot.resolve("PjbPublicApi.java"), "annotation X {}\n");
        Files.writeString(moduleRoot.resolve("PjbPublicApi.java"), "annotation X {}\n");
        var service = new PjbCoreSeedExtractionApplicationService(mock(AuditLedgerService.class), tempDir);

        var parity = service.parity();

        assertThat(parity).hasSize(1);
        assertThat(parity.getFirst().className()).isEqualTo("PjbPublicApi.java");
        assertThat(parity.getFirst().contentAligned()).isTrue();
    }

    @Test
    void drift_deveApontarHashMismatchQuandoEspelhoDivergir() throws Exception {
        Path sourceRoot = tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/modularity");
        Path moduleRoot = tempDir.resolve("pjb-core/src/main/java/com/tcc/pjb/backend/core/modularity");
        Files.createDirectories(sourceRoot);
        Files.createDirectories(moduleRoot);
        Files.writeString(sourceRoot.resolve("PjbInternal.java"), "class A {}\n");
        Files.writeString(moduleRoot.resolve("PjbInternal.java"), "class B {}\n");
        var service = new PjbCoreSeedExtractionApplicationService(mock(AuditLedgerService.class), tempDir);

        var drift = service.drift();

        assertThat(drift).hasSize(1);
        assertThat(drift.getFirst().issueType()).isEqualTo("content.hash.mismatch");
    }
}
