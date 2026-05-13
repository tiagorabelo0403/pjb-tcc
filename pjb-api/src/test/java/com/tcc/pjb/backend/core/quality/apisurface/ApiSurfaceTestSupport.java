package com.tcc.pjb.backend.core.quality.apisurface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class ApiSurfaceTestSupport {

    static final Path MAIN_JAVA = Path.of("src/main/java");
    static final Path APPLICATION_YML = Path.of("src/main/resources/application.yml");

    private ApiSurfaceTestSupport() {
    }

    static List<Path> controllerFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .toList();
        }
    }

    static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo de teste: " + path, ex);
        }
    }

    static String applicationYaml() {
        return read(APPLICATION_YML);
    }
}
