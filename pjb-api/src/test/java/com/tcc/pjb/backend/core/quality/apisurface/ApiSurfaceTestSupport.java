package com.tcc.pjb.backend.core.quality.apisurface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class ApiSurfaceTestSupport {

    static final Path MODULE_ROOT = locateModuleRoot();
    static final Path MAIN_JAVA = Path.of("src/main/java");
    static final Path APPLICATION_YML = Path.of("src/main/resources/application.yml");
    static final Path API_GOVERNANCE_YML = Path.of("src/main/resources/application-api-governance.yml");

    private ApiSurfaceTestSupport() {
    }

    static List<Path> controllerFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(MODULE_ROOT.resolve(MAIN_JAVA))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .map(path -> MODULE_ROOT.relativize(path.toAbsolutePath().normalize()))
                    .sorted()
                    .toList();
        }
    }

    static String read(Path path) {
        try {
            return Files.readString(path.isAbsolute() ? path : MODULE_ROOT.resolve(path));
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo de teste: " + path, ex);
        }
    }

    static String applicationYaml() {
        return read(APPLICATION_YML) + "\n" + read(API_GOVERNANCE_YML);
    }

    private static Path locateModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(cwd.resolve("src/main/java"))) {
            return cwd;
        }
        Path module = cwd.resolve("pjb-api");
        if (Files.isDirectory(module.resolve("src/main/java"))) {
            return module;
        }
        throw new IllegalStateException("Nao foi possivel localizar o modulo pjb-api a partir de " + cwd);
    }
}
