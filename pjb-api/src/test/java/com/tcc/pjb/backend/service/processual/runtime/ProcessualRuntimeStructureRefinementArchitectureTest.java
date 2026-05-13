package com.tcc.pjb.backend.service.processual.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProcessualRuntimeStructureRefinementArchitectureTest {

    private static final Path SERVICE_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/runtime");
    private static final Path DTO_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/runtime");

    @Test
    void runtimeClusterMustKeepGuardAndHomologationSubpackages() {
        assertThat(Files.isDirectory(SERVICE_ROOT.resolve("guard"))).isTrue();
        assertThat(Files.isDirectory(SERVICE_ROOT.resolve("homologation"))).isTrue();
        assertThat(Files.isDirectory(DTO_ROOT.resolve("guard"))).isTrue();
        assertThat(Files.isDirectory(DTO_ROOT.resolve("homologation"))).isTrue();
    }

    @Test
    void runtimeRootsMustNotKeepJavaFilesAfterRefinement() throws Exception {
        assertJavaRootEmpty(SERVICE_ROOT);
        assertJavaRootEmpty(DTO_ROOT);
    }

    private void assertJavaRootEmpty(Path root) throws Exception {
        try (var stream = Files.list(root)) {
            assertThat(stream.filter(path -> path.getFileName().toString().endsWith(".java"))).isEmpty();
        }
    }
}
