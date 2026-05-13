package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingPhaseExtractionGovernanceTest {

    @Test
    void mustKeepMainServiceAsPhaseOrchestrator() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String coreAnalyzer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingCoreAnalyzer.java"));
        String foundationAnalyzer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingFoundationAnalyzer.java"));
        String classificationAnalyzer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingClassificationAnalyzer.java"));
        String placementReviewResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingPlacementReviewResolver.java"));
        String finalizer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingFinalizationResolver.java"));

        assertTrue(service.contains("coreAnalyzer.analyze("));
        assertTrue(service.contains("finalizationResolver.finalize("));
        assertFalse(service.contains("canonicalRitoSelector.select("));
        assertFalse(service.contains("competenceResolverService.resolve("));
        assertFalse(service.contains("reviewSynthesisResolver.resolve("));
        assertFalse(service.contains("economicGateFactory.build("));
        assertFalse(service.contains("metadataFactory.build("));

        assertTrue(coreAnalyzer.contains("foundationAnalyzer.analyze("));
        assertTrue(coreAnalyzer.contains("classificationAnalyzer.analyze("));
        assertTrue(foundationAnalyzer.contains("canonicalRitoSelector.select("));
        assertTrue(foundationAnalyzer.contains("competenceResolverService.resolve("));
        assertTrue(classificationAnalyzer.contains("placementReviewResolver.resolve("));
        assertTrue(placementReviewResolver.contains("reviewSynthesisResolver.resolve("));
        assertTrue(finalizer.contains("economicGateFactory.build("));
        assertTrue(finalizer.contains("metadataContextFactory.create("));
        assertTrue(finalizer.contains("metadataFactory.build("));
        assertTrue(finalizer.contains("reportAssemblyContextFactory.create("));
    }
}
