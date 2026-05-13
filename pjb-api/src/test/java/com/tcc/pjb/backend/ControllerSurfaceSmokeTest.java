package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ControllerSurfaceSmokeTest {

    private static final Pattern HTTP_MAPPING = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)");

    @Test
    void deveDetectarSurfaceHttpMinimaDaBase() throws IOException {
        Path root = Path.of("src/main/java/com/tcc/pjb/backend").toAbsolutePath().normalize();
        AtomicInteger controllerCount = new AtomicInteger();
        AtomicInteger mappingCount = new AtomicInteger();

        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .forEach(path -> {
                        controllerCount.incrementAndGet();
                        try {
                            String text = Files.readString(path, StandardCharsets.UTF_8);
                            var matcher = HTTP_MAPPING.matcher(text);
                            while (matcher.find()) {
                                mappingCount.incrementAndGet();
                            }
                        } catch (IOException ignored) {
                        }
                    });
        }

        assertThat(controllerCount.get()).isGreaterThan(100);
        assertThat(mappingCount.get()).isGreaterThan(300);
    }
}
