package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingReviewSynthesisSubphasesGovernanceTest {

    @Test
    void mustKeepReviewSynthesisResolverAsOrchestratorBetweenSignalsInputRequirementsAndConfidence() throws Exception {
        String placementReviewResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingPlacementReviewResolver.java"));
        String synthesisResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralReviewSynthesisResolver.java"));
        String signalCollector = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralReviewSignalCollector.java"));
        String inputRequirementResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralReviewInputRequirementResolver.java"));

        assertTrue(placementReviewResolver.contains("reviewSynthesisResolver.resolve("));
        assertTrue(synthesisResolver.contains("reviewSignalCollector.collect("));
        assertTrue(synthesisResolver.contains("reviewInputRequirementResolver.assess("));
        assertTrue(synthesisResolver.contains("confidenceResolver.assess("));
        assertFalse(synthesisResolver.contains("payload.get(\"classe\")"));
        assertFalse(synthesisResolver.contains("messages.missingClasseBlocking()"));
        assertTrue(signalCollector.contains("reviewReasonCollector.collect("));
        assertTrue(signalCollector.contains("reviewPolicySignalResolver.collect("));
        assertFalse(signalCollector.contains("context.competence().reasons()"));
        assertTrue(inputRequirementResolver.contains("coreFieldRequirementResolver.assess("));
        assertTrue(inputRequirementResolver.contains("economicRequirementResolver.assess("));
        assertTrue(inputRequirementResolver.contains("locationRequirementResolver.assess("));
        assertTrue(inputRequirementResolver.contains("partyRequirementResolver.assess("));
        assertFalse(inputRequirementResolver.contains("payload.get(\"classe\")"));
    }
}
