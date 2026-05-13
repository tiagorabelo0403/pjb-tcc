package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingCoreSubphasesGovernanceTest {

    @Test
    void mustKeepCoreAnalyzerAsSubphaseOrchestratorAndNotAsMaterialConcentrator() throws Exception {
        String coreAnalyzer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingCoreAnalyzer.java"));
        String foundationAnalyzer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingFoundationAnalyzer.java"));
        String classificationAnalyzer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingClassificationAnalyzer.java"));
        String trackClassificationResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingTrackClassificationResolver.java"));
        String placementReviewResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingPlacementReviewResolver.java"));

        assertTrue(coreAnalyzer.contains("foundationAnalyzer.analyze("));
        assertTrue(coreAnalyzer.contains("classificationAnalyzer.analyze("));
        assertFalse(coreAnalyzer.contains("canonicalRitoSelector.select("));
        assertFalse(coreAnalyzer.contains("competenceResolverService.resolve("));
        assertFalse(coreAnalyzer.contains("reviewSynthesisResolver.resolve("));
        assertFalse(coreAnalyzer.contains("judicialPlacementResolver.resolve("));

        assertTrue(foundationAnalyzer.contains("canonicalRitoSelector.select("));
        assertTrue(foundationAnalyzer.contains("competenceResolverService.resolve("));
        assertTrue(foundationAnalyzer.contains("juizadoDecisionResolver.resolve("));
        assertTrue(classificationAnalyzer.contains("trackClassificationResolver.resolve("));
        assertTrue(classificationAnalyzer.contains("placementReviewResolver.resolve("));
        assertTrue(trackClassificationResolver.contains("complexityBandResolver.resolve("));
        assertTrue(placementReviewResolver.contains("judicialPlacementResolver.resolve("));
        assertTrue(placementReviewResolver.contains("reviewSynthesisResolver.resolve("));
    }
}
