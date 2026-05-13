package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingClassificationSubphasesGovernanceTest {

    @Test
    void mustKeepClassificationAnalyzerAsOrchestratorBetweenTrackClassificationAndPlacementReview() throws Exception {
        String classificationAnalyzer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingClassificationAnalyzer.java"));
        String trackClassificationResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingTrackClassificationResolver.java"));
        String placementReviewResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingPlacementReviewResolver.java"));

        assertTrue(classificationAnalyzer.contains("trackClassificationResolver.resolve("));
        assertTrue(classificationAnalyzer.contains("placementReviewResolver.resolve("));
        assertFalse(classificationAnalyzer.contains("complexityBandResolver.resolve("));
        assertFalse(classificationAnalyzer.contains("tipoJusticaResolver.resolve("));
        assertFalse(classificationAnalyzer.contains("judicialPlacementResolver.resolve("));
        assertFalse(classificationAnalyzer.contains("reviewSynthesisResolver.resolve("));

        assertTrue(trackClassificationResolver.contains("complexityBandResolver.resolve("));
        assertTrue(trackClassificationResolver.contains("tipoJusticaResolver.resolve("));
        assertTrue(trackClassificationResolver.contains("classificationResolver.resolveProceduralRegime("));
        assertTrue(placementReviewResolver.contains("judicialPlacementResolver.resolve("));
        assertTrue(placementReviewResolver.contains("reviewSynthesisResolver.resolve("));
    }
}
