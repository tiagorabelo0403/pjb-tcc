package com.tcc.pjb.backend.core.quality.codebase.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class PjbProjectPathResolver {

    private PjbProjectPathResolver() {
    }

    public static Path workspaceRoot(Path candidate) {
        Path normalized = normalize(candidate == null ? Path.of("") : candidate);
        if (Files.isRegularFile(normalized.resolve("pom.xml")) && Files.isDirectory(normalized.resolve("pjb-api"))) {
            return normalized;
        }
        Path parent = normalized.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve("pom.xml")) && Files.isDirectory(parent.resolve("pjb-api"))) {
            return parent;
        }
        return normalized;
    }

    public static Path apiModuleRoot(Path candidate) {
        Path workspace = workspaceRoot(candidate);
        Path module = workspace.resolve("pjb-api");
        if (Files.isDirectory(module.resolve("src/main/java"))) {
            return module;
        }
        if (Files.isDirectory(workspace.resolve("src/main/java"))
                || Files.isDirectory(workspace.resolve("src/main/resources"))
                || Files.isDirectory(workspace.resolve("src/test/java"))
                || Files.isDirectory(workspace.resolve("src/test/resources"))) {
            return workspace;
        }
        return module;
    }

    public static Path appMainJava(Path candidate) {
        return apiModuleRoot(candidate).resolve("src/main/java");
    }

    public static Path appTestJava(Path candidate) {
        return apiModuleRoot(candidate).resolve("src/test/java");
    }

    public static Path appMainResources(Path candidate) {
        return apiModuleRoot(candidate).resolve("src/main/resources");
    }

    public static Path appTestResources(Path candidate) {
        return apiModuleRoot(candidate).resolve("src/test/resources");
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path).toAbsolutePath().normalize();
    }
}
