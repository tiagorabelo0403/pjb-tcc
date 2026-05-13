package com.tcc.pjb.backend.controller.processual.recursal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProcessualRecursalControllerStructureRefinementArchitectureTest {

    private static final Path ROOT = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal");

    @Test
    void shouldKeepRecursalControllersOrganizedInDedicatedSubpackages() throws IOException {
        assertTrue(Files.isDirectory(ROOT.resolve("admissibilidade")));
        assertTrue(Files.isDirectory(ROOT.resolve("ia")));
        try (Stream<Path> stream = Files.list(ROOT)) {
            long javaFiles = stream
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .count();
            assertEquals(0, javaFiles);
        }
    }
}
