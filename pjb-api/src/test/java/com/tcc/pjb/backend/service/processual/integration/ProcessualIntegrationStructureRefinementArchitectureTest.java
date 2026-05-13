package com.tcc.pjb.backend.service.processual.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProcessualIntegrationStructureRefinementArchitectureTest {

    private static final Path SERVICE_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/integration");
    private static final Path CONTROLLER_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/integration");
    private static final Path DTO_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/integration");

    @Test
    void integrationClusterMustKeepExternalAndIntertribunalSubpackages() {
        assertThat(Files.isDirectory(SERVICE_ROOT.resolve("external"))).isTrue();
        assertThat(Files.isDirectory(SERVICE_ROOT.resolve("intertribunal"))).isTrue();
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("external"))).isTrue();
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("intertribunal"))).isTrue();
        assertThat(Files.isDirectory(DTO_ROOT.resolve("external"))).isTrue();
        assertThat(Files.isDirectory(DTO_ROOT.resolve("intertribunal"))).isTrue();
    }

    @Test
    void integrationRootsMustNotKeepJavaFilesAfterRefinement() throws Exception {
        assertJavaRootEmpty(SERVICE_ROOT);
        assertJavaRootEmpty(CONTROLLER_ROOT);
        assertJavaRootEmpty(DTO_ROOT);
    }

    private void assertJavaRootEmpty(Path root) throws Exception {
        try (var stream = Files.list(root)) {
            assertThat(stream.filter(path -> path.getFileName().toString().endsWith(".java"))).isEmpty();
        }
    }
}
