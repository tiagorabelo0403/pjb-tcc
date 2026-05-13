package com.tcc.pjb.backend.controller.processual.playbook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProcessualPlaybookControllerStructureRefinementArchitectureTest {

    private static final Path ROOT = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/playbook");

    @Test
    void playbookControllerClusterMustKeepOperationalAndVariationSubpackages() {
        assertThat(Files.isDirectory(ROOT.resolve("operational"))).isTrue();
        assertThat(Files.isDirectory(ROOT.resolve("variation"))).isTrue();
    }

    @Test
    void playbookControllerRootMustNotKeepJavaFilesAfterRefinement() throws Exception {
        try (var stream = Files.list(ROOT)) {
            assertThat(stream.filter(path -> path.getFileName().toString().endsWith(".java"))).isEmpty();
        }
    }
}
