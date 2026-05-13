package com.tcc.pjb.backend.controller.processual.participacao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProcessualParticipacaoControllerStructureRefinementArchitectureTest {

    private static final Path ROOT = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/participacao");

    @Test
    void participacaoControllerDeveFicarSeparadoEntreWorkspaceSubmissionESupport() {
        assertAllSubpackage("workspace");
        assertAllSubpackage("submission");
        assertAllSubpackage("support");
    }

    @Test
    void raizNaoDeveConterJavaResidual() throws IOException {
        try (Stream<Path> stream = Files.list(ROOT)) {
            boolean hasJava = stream
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.getFileName().toString().endsWith(".java"));
            assertFalse(hasJava, "Raiz de controller/processual/participacao ainda contém .java residual");
        }
    }

    private void assertAllSubpackage(String relative) {
        Path path = ROOT.resolve(relative);
        assertTrue(Files.isDirectory(path), "Subpacote ausente: " + path);
    }
}
