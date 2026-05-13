package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PjbControllerPreAuthorizeCoverageTest {

    @Test
    void allControllersMustDeclareExplicitPreAuthorize() throws Exception {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java"))) {
            List<String> missing = stream
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .filter(Files::isRegularFile)
                    .filter(path -> isController(path))
                    .filter(path -> !containsPreAuthorize(path))
                    .map(Path::toString)
                    .sorted()
                    .toList();
            assertTrue(missing.isEmpty(), "Controllers sem @PreAuthorize explicito: " + missing);
        }
    }

    private static boolean isController(Path path) {
        try {
            String content = Files.readString(path);
            return content.contains("@RestController") || content.contains("@Controller");
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean containsPreAuthorize(Path path) {
        try {
            return Files.readString(path).contains("@PreAuthorize");
        } catch (Exception ex) {
            return false;
        }
    }
}
