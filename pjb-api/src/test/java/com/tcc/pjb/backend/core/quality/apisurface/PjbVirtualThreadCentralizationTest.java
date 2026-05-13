package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PjbVirtualThreadCentralizationTest {

    @Test
    void directVirtualThreadApisMustStayCentralizedInPjbVirtualThreadSpine() throws Exception {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java"))) {
            List<String> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsDirectVirtualThreadApi)
                    .map(path -> path.toString().replace('\\', '/'))
                    .sorted()
                    .toList();
            assertEquals(List.of(
                    "src/main/java/com/tcc/pjb/backend/core/quality/codebase/application/PjbCodebaseSanitySnapshotBuilder.java",
                    "src/main/java/com/tcc/pjb/backend/platform/concurrent/PjbVirtualThreadSpine.java"
            ), files);
        }
    }

    private boolean containsDirectVirtualThreadApi(Path path) {
        String content = ApiSurfaceTestSupport.read(path);
        return content.contains("Thread.ofVirtual()")
                || content.contains("Thread.startVirtualThread(")
                || content.contains("newThreadPerTaskExecutor(")
                || content.contains("newVirtualThreadPerTaskExecutor(")
                || content.contains(".virtualThreads(true)");
    }
}
