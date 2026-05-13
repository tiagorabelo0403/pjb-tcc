package com.tcc.pjb.backend.governance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class DuplicateClassNameGuardTest {

    @Test
    void noDuplicateJavaBasenameInMain() throws IOException {
        Path root = Paths.get("src/main/java");
        if (!Files.exists(root)) {
            
            return;
        }

        Map<String, List<Path>> byName = new HashMap<>();
        try (var stream = Files.walk(root)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> !"package-info.java".equals(p.getFileName().toString()))
                    .forEach(p -> byName.computeIfAbsent(p.getFileName().toString(), k -> new ArrayList<>()).add(p));
        }

        List<String> duplicates = new ArrayList<>();
        for (var e : byName.entrySet()) {
            if (e.getValue().size() > 1) {
                duplicates.add(e.getKey() + " -> " + e.getValue());
            }
        }

        assertTrue(duplicates.isEmpty(),
                "Arquivos .java duplicados por nome (basename) encontrados em src/main/java:\n"
                        + String.join("\n", duplicates));
    }
}
