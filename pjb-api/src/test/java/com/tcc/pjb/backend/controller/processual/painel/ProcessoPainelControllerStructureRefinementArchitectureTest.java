package com.tcc.pjb.backend.controller.processual.painel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProcessoPainelControllerStructureRefinementArchitectureTest {

    private static final Path CONTROLLER_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/painel");

    @Test
    void painelControllerClusterMustKeepDedicatedSubpackages() {
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("contextual"))).isTrue();
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("detalhe"))).isTrue();
    }

    @Test
    void painelControllerRootMustNotKeepJavaFilesAfterRefinement() throws Exception {
        try (var stream = Files.list(CONTROLLER_ROOT)) {
            assertThat(stream.filter(path -> path.getFileName().toString().endsWith(".java"))).isEmpty();
        }
    }
}
