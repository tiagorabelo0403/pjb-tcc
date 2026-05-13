package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingClassificationAndForumExtractionGovernanceTest {

    @Test
    void mustKeepClassificationAndForumLabelExtractionOutOfMainService() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String classificationResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralClassificationResolver.java"));
        String forumLabelFactory = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumLabelFactory.java"));

        assertFalse(service.contains("private String resolveProceduralRegime("));
        assertFalse(service.contains("private String resolveProceduralTrack("));
        assertFalse(service.contains("private String buildForoLabel("));
        assertFalse(service.contains("private String buildVaraLabel("));

        assertTrue(classificationResolver.contains("String resolveProceduralRegime("));
        assertTrue(classificationResolver.contains("String resolveProceduralTrack("));
        assertTrue(forumLabelFactory.contains("String buildForoLabel("));
        assertTrue(forumLabelFactory.contains("String buildVaraLabel("));
    }
}
