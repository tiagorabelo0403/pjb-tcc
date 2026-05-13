package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingDecisionAndForumExtractionGovernanceTest {

    @Test
    void mustKeepForumAllocationAndDecisionSynthesisOutOfMainServiceAndInsideCoreSubphases() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String placementReviewResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingPlacementReviewResolver.java"));
        String forumAllocationResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationResolver.java"));
        String forumSeedResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationSeedResolver.java"));
        String forumReadinessResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumRoutingReadinessResolver.java"));
        String forumAssembler = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationReportAssembler.java"));
        String reviewSynthesisResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralReviewSynthesisResolver.java"));
        String confidenceResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralConfidenceResolver.java"));

        assertFalse(service.contains("forumAllocationResolver.resolve("));
        assertFalse(service.contains("reviewSynthesisResolver.resolve("));
        assertTrue(service.contains("coreAnalyzer.analyze("));

        assertTrue(placementReviewResolver.contains("reviewSynthesisResolver.resolve("));
        assertTrue(placementReviewResolver.contains("judicialPlacementResolver.resolve("));
        assertTrue(forumAllocationResolver.contains("seedResolver.resolve("));
        assertTrue(forumAllocationResolver.contains("readinessResolver.resolve("));
        assertTrue(forumAllocationResolver.contains("reportAssembler.assemble("));
        assertFalse(forumAllocationResolver.contains("tribunalProtocolRoutingService.resolve("));
        assertFalse(forumAllocationResolver.contains("proceduralPreflightEngine.evaluate("));
        assertTrue(forumSeedResolver.contains("resolveFallbackPerfil("));
        assertTrue(forumReadinessResolver.contains("tribunalProtocolRoutingService.resolve("));
        assertTrue(forumReadinessResolver.contains("proceduralPreflightEngine.evaluate("));
        assertTrue(forumAssembler.contains("new ProceduralForumAllocationReport("));
        assertTrue(reviewSynthesisResolver.contains("NationalProceduralReviewSynthesis resolve("));
        assertTrue(confidenceResolver.contains("NationalProceduralConfidenceAssessment assess("));
    }
}
