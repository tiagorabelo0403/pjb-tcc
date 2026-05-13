package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BootstrapCascadeBarrierArchitectureTest {

    @Test
    void componentesComScheduledNaoDevemSerFinalParaEvitarFalhaDeProxyNoBootstrap() throws IOException {
        Path root = Path.of(System.getProperty("user.dir")).resolve("src/main/java");
        if (!Files.exists(root)) {
            return;
        }
        List<String> violations = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> inspect(path, violations));
        }
        assertTrue(violations.isEmpty(), () -> "Classes @Scheduled não podem ser final: " + violations);
    }

    private static void inspect(Path path, List<String> violations) {
        try {
            String source = Files.readString(path);
            if (!source.contains("@Scheduled") || !source.contains("class ")) {
                return;
            }
            String header = source.split("\\{", 2)[0];
            if (header.matches("(?s).*\\bpublic\\s+final\\s+class\\s+\\w+.*")
                || header.matches("(?s).*\\bfinal\\s+class\\s+\\w+.*")) {
                violations.add(path.getFileName().toString());
            }
        } catch (IOException ignored) {
        }
    }
}
