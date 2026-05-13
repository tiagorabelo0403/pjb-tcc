package com.tcc.pjb.backend.core.quality.codebase.application;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record PjbCodebaseSanityProjectLayout(
        Path projectRoot,
        Path mainSourceRoot,
        Path testSourceRoot,
        Path pomFile
) {
    public PjbCodebaseSanityProjectLayout {
        projectRoot = normalize(projectRoot);
        mainSourceRoot = normalize(mainSourceRoot);
        testSourceRoot = normalize(testSourceRoot);
        pomFile = normalize(pomFile);
    }

    public static PjbCodebaseSanityProjectLayout fromProjectRoot(Path projectRoot) {
        Path normalizedRoot = PjbProjectPathResolver.workspaceRoot(projectRoot);
        Path appRoot = PjbProjectPathResolver.apiModuleRoot(projectRoot);
        return new PjbCodebaseSanityProjectLayout(
                normalizedRoot,
                appRoot.resolve("src/main/java"),
                appRoot.resolve("src/test/java"),
                normalizedRoot.resolve("pom.xml")
        );
    }

    public List<Path> sourceRoots() {
        Path workspace = projectRoot;
        Path coreMain = workspace.resolve("pjb-core/src/main/java");
        Path coreTest = workspace.resolve("pjb-core/src/test/java");
        return List.of(mainSourceRoot, testSourceRoot, coreMain, coreTest);
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path).toAbsolutePath().normalize();
    }
}
