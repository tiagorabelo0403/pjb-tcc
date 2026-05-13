package com.tcc.pjb.backend.service.processual.postarchive;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PostArchivePolicyStructureRefinementArchitectureTest {

    private static final Path ROOT = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/postarchive");

    @Test
    void postArchiveClusterMustKeepVisibilityAndTombstoneSubpackages() {
        assertThat(Files.isDirectory(ROOT.resolve("visibility"))).isTrue();
        assertThat(Files.isDirectory(ROOT.resolve("tombstone"))).isTrue();
    }

    @Test
    void postArchiveRootMustKeepOnlyEntryServicesAfterPolicyRefinement() throws Exception {
        try (var stream = Files.list(ROOT)) {
            assertThat(stream
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString()))
                    .containsExactlyInAnyOrder(
                            "PostArchiveAccessRequestService.java",
                            "PostArchiveLifecycleService.java");
        }
    }
}
