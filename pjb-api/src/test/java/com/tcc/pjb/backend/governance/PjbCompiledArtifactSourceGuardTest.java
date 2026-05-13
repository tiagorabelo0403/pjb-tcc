package com.tcc.pjb.backend.governance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PjbCompiledArtifactSourceGuardTest {

    @Test
    void sourceTreeNaoDeveConterArtefatosCompilados() throws IOException {
        List<Path> roots = List.of(Path.of("src/main/java"), Path.of("src/test/java"));
        try (Stream<Path> stream = roots.stream().filter(Files::exists).flatMap(this::walkSafe)) {
            List<String> compiledArtifacts = stream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(path -> path.endsWith(".class"))
                    .sorted()
                    .toList();

            assertTrue(compiledArtifacts.isEmpty(), "Artefatos compilados não devem existir dentro de src: " + compiledArtifacts);
        }
    }

    private Stream<Path> walkSafe(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao varrer árvore de fontes: " + root, ex);
        }
    }
}
