package com.tcc.pjb.backend.core.quality.codebase.application;

import java.nio.file.Path;
import java.util.Objects;

public record PjbCodebaseProjectLayout(
        Path projectRoot,
        Path mainRoot,
        Path testRoot
) {
    public PjbCodebaseProjectLayout {
        projectRoot = normalize(projectRoot);
        mainRoot = normalize(mainRoot);
        testRoot = normalize(testRoot);
    }

    public static PjbCodebaseProjectLayout fromProjectRoot(Path projectRoot) {
        Path normalizedRoot = PjbProjectPathResolver.workspaceRoot(projectRoot);
        Path appRoot = PjbProjectPathResolver.apiModuleRoot(projectRoot);
        return new PjbCodebaseProjectLayout(
                normalizedRoot,
                appRoot.resolve("src/main/java/com/tcc/pjb/backend"),
                appRoot.resolve("src/test/java/com/tcc/pjb/backend")
        );
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path).toAbsolutePath().normalize();
    }
}
