package com.tcc.pjb.backend.service.processual.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProcessualValidationStructureRefinementArchitectureTest {

    private static final Path SERVICE_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/validation");
    private static final Path CONTROLLER_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/validation");
    private static final Path DTO_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/validation");

    @Test
    void validationClusterMustKeepMaterialSubpackage() {
        assertThat(Files.isDirectory(SERVICE_ROOT.resolve("material"))).isTrue();
        assertThat(Files.isDirectory(CONTROLLER_ROOT.resolve("material"))).isTrue();
        assertThat(Files.isDirectory(DTO_ROOT.resolve("material"))).isTrue();
    }

    @Test
    void validationRootsMustNotKeepJavaFilesAfterRefinement() throws Exception {
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
