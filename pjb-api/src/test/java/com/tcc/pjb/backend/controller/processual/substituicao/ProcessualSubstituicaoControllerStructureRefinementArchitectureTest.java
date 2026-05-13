package com.tcc.pjb.backend.controller.processual.substituicao;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProcessualSubstituicaoControllerStructureRefinementArchitectureTest {

    private static final Path CONTROLLER_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/substituicao");

    @Test
    void substituicaoControllerClusterMustKeepDedicatedSubpackages() {
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("arquitetura"))).isTrue();
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("legados"))).isTrue();
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("nacional"))).isTrue();
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("routes"))).isTrue();
    }

    @Test
    void substituicaoControllerRootMustNotKeepJavaFilesAfterRefinement() throws Exception {
        try (var stream = Files.list(CONTROLLER_ROOT)) {
            assertThat(stream.filter(path -> path.getFileName().toString().endsWith(".java"))).isEmpty();
        }
    }
}
