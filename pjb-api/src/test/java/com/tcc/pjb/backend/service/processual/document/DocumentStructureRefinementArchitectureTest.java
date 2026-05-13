package com.tcc.pjb.backend.service.processual.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DocumentStructureRefinementArchitectureTest {

    private static final Path ROOT = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/document");

    @Test
    void documentClusterMustKeepIdentityEnvelopeAndTemplateSubpackages() throws Exception {
        assertThat(Files.isDirectory(ROOT.resolve("identity"))).isTrue();
        assertThat(Files.isDirectory(ROOT.resolve("envelope"))).isTrue();
        assertThat(Files.isDirectory(ROOT.resolve("template"))).isTrue();
    }

    @Test
    void documentRootMustNotKeepJavaFilesAfterRefinement() throws Exception {
        try (var stream = Files.list(ROOT)) {
            assertThat(stream.filter(path -> path.getFileName().toString().endsWith(".java"))).isEmpty();
        }
    }
}
