package com.tcc.pjb.backend.governance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DuplicateTestClassNameGuardTest {

    @Test
    void noDuplicateJavaBasenameInTestSources() throws IOException {
        Path root = Path.of("src/test/java");
        if (!Files.exists(root)) {
            return;
        }

        Map<String, List<Path>> byName = new HashMap<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !"package-info.java".equals(path.getFileName().toString()))
                    .forEach(path -> byName.computeIfAbsent(path.getFileName().toString(), key -> new ArrayList<>()).add(path));
        }

        List<String> duplicates = new ArrayList<>();
        byName.forEach((name, paths) -> {
            if (paths.size() > 1) {
                duplicates.add(name + " -> " + paths);
            }
        });

        assertTrue(duplicates.isEmpty(), "Arquivos de teste duplicados por basename encontrados em src/test/java: " + duplicates);
    }
}
