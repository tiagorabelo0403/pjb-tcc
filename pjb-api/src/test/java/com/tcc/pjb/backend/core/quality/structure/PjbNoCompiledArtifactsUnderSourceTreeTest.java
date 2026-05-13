package com.tcc.pjb.backend.core.quality.structure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbNoCompiledArtifactsUnderSourceTreeTest {

    @Test
    void shouldNotContainCompiledArtifactsInsideMainJavaTree() throws IOException {
        Path root = Path.of("src", "main", "java");
        if (!Files.exists(root)) {
            return;
        }
        List<Path> compiledArtifacts = Files.walk(root)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".class"))
                .toList();
        assertTrue(compiledArtifacts.isEmpty(), () -> "compiled artifacts found under src/main/java: " + compiledArtifacts);
    }
}
