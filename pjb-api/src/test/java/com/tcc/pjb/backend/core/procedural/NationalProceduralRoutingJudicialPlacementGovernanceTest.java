package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingJudicialPlacementGovernanceTest {

    @Test
    void mustKeepJudicialPlacementOutOfMainServiceAndSplitIntoSeedAndFinalizer() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String placementReviewResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingPlacementReviewResolver.java"));
        String resolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralJudicialPlacementResolver.java"));
        String seedResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralJudicialPlacementSeedResolver.java"));
        String finalizer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralJudicialPlacementFinalizer.java"));

        assertFalse(service.contains("judicialPlacementResolver.resolve("));
        assertTrue(service.contains("coreAnalyzer.analyze("));
        assertTrue(placementReviewResolver.contains("judicialPlacementResolver.resolve("));
        assertTrue(resolver.contains("seedResolver.resolve("));
        assertTrue(resolver.contains("finalizer.finalizePlacement("));
        assertFalse(resolver.contains("distributionResolver.resolve("));
        assertFalse(resolver.contains("forumAllocationResolver.resolve("));
        assertTrue(seedResolver.contains("distributionResolver.resolve("));
        assertTrue(finalizer.contains("forumAllocationResolver.resolve("));
        assertTrue(finalizer.contains("forumLabelFactory.buildForoLabel("));
    }
}
