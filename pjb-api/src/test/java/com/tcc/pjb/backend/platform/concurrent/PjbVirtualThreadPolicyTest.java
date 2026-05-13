package com.tcc.pjb.backend.platform.concurrent;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class PjbVirtualThreadPolicyTest {

    private static final Path ROOT = Path.of("src/main/java");
    private static final Path SPINE = Path.of("src/main/java/com/tcc/pjb/backend/platform/concurrent/PjbVirtualThreadSpine.java");
    private static final Path CODEBASE_SNAPSHOT_BUILDER = Path.of("src/main/java/com/tcc/pjb/backend/core/quality/codebase/application/PjbCodebaseSanitySnapshotBuilder.java");

    @Test
    void mustNotUseVirtualThreadsOutsideTheSpine() throws Exception {
        try (Stream<Path> files = Files.walk(ROOT)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.equals(SPINE))
                    .filter(path -> !path.equals(CODEBASE_SNAPSHOT_BUILDER))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path, StandardCharsets.UTF_8);
                            assertFalse(content.contains("Thread.ofVirtual().start("), () -> "Found direct virtual thread start in " + path);
                            assertFalse(content.contains(".virtualThreads(true)"), () -> "Found Spring virtualThreads usage in " + path);
                            assertFalse(content.contains("Executors.newThreadPerTaskExecutor(Thread.ofVirtual("), () -> "Found direct virtual executor usage in " + path);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }
}
