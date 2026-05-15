package com.tcc.pjb.backend.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PjbRepositoryLayoutGovernanceTest {

    @Test
    void repository_should_not_expose_transient_or_ide_noise() throws IOException {
        Path root = detectRoot();
        Path gitignore = root.resolve(".gitignore");
        if (Files.exists(gitignore)) {
            String content = Files.readString(gitignore);
            assertFalse(!content.contains(".idea"), ".idea/ must be listed in .gitignore");
            assertFalse(!content.contains("*.iml"), "*.iml must be listed in .gitignore");
        }
        try (Stream<Path> stream = Files.walk(root, 4)) {
            List<Path> offenders = stream
                    .filter(path -> path.getFileName() != null)
                    .filter(path -> !path.toString().contains("/target/") && !path.toString().contains("\\target\\") && !path.toString().contains("/build/") && !path.toString().contains("\\build\\") && !path.toString().contains("/.idea/") && !path.toString().contains("\\.idea\\") && !path.toString().contains("__pycache__"))
                    .filter(path -> path.getFileName().toString().equals("__pycache__") || path.getFileName().toString().endsWith(".iml"))
                    .toList();
            assertFalse(!offenders.isEmpty(), "Transient files found: " + offenders);
        }
    }

    @Test
    void repository_root_should_not_host_round_markdown_noise() throws IOException {
        Path root = detectRoot();
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> offenders = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .filter(path -> !path.getFileName().toString().equals("README.md") && !path.getFileName().toString().equals("CLAUDE.md"))
                    .toList();
            assertFalse(!offenders.isEmpty(), "Unexpected markdown files in root: " + offenders);
        }
    }

    private Path detectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("pom.xml")) && Files.exists(cwd.resolve("pjb-api"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("pom.xml")) && Files.exists(parent.resolve("pjb-api"))) {
            return parent;
        }
        throw new IllegalStateException("Repository root not found from " + cwd);
    }
}
