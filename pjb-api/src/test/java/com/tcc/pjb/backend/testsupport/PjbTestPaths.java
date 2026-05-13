package com.tcc.pjb.backend.testsupport;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PjbTestPaths {

    private PjbTestPaths() {
    }

    public static Path projectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("pom.xml")) && Files.exists(cwd.resolve("pjb-api"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("pom.xml")) && Files.exists(parent.resolve("pjb-api"))) {
            return parent;
        }
        return cwd;
    }


    public static Path workspaceRoot() {
        return projectRoot();
    }

    public static Path pjbApiRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("src/main/java")) && Files.exists(cwd.resolve("pom.xml")) && "pjb-api".equals(cwd.getFileName().toString())) {
            return cwd;
        }
        Path workspace = projectRoot();
        Path module = workspace.resolve("pjb-api");
        if (Files.exists(module.resolve("pom.xml"))) {
            return module;
        }
        return cwd;
    }

    public static Path pjbApiMainJavaRoot() {
        return pjbApiRoot().resolve("src/main/java").normalize();
    }

    public static Path pjbApiTestJavaRoot() {
        return pjbApiRoot().resolve("src/test/java").normalize();
    }

    public static Path backendMainRoot() {
        return pjbApiMainJavaRoot().resolve("com/tcc/pjb/backend").normalize();
    }

    public static Path backendTestRoot() {
        return pjbApiTestJavaRoot().resolve("com/tcc/pjb/backend").normalize();
    }

    public static Path pjbApiMainResourcesRoot() {
        return pjbApiRoot().resolve("src/main/resources").normalize();
    }

    public static Path pjbApiTestResourcesRoot() {
        return pjbApiRoot().resolve("src/test/resources").normalize();
    }

    private static Path resolve(String moduleRelative, String projectRelative) {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path first = cwd.resolve(moduleRelative).normalize();
        if (Files.exists(first)) {
            return first;
        }
        Path second = cwd.resolve(projectRelative).normalize();
        if (Files.exists(second)) {
            return second;
        }
        throw new IllegalStateException("Nao foi possivel resolver caminho de teste a partir de " + cwd);
    }
}
