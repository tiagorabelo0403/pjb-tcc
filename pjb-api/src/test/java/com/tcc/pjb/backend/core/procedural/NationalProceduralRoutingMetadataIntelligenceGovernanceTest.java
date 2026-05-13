package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingMetadataIntelligenceGovernanceTest {

    @Test
    void mustKeepMetadataFactoryAsOrchestratorAndMovePipelineToDedicatedResolver() throws Exception {
        String metadataFactory = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingMetadataFactory.java"));
        String seedFactory = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingMetadataSeedFactory.java"));
        String intelligenceResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingIntelligenceResolver.java"));

        assertTrue(metadataFactory.contains("metadataSeedFactory.build("));
        assertTrue(metadataFactory.contains("intelligenceResolver.analyze("));
        assertFalse(metadataFactory.contains("ProceduralIntelligenceAdvisor.analyzeRouting("));
        assertFalse(metadataFactory.contains("ProceduralDecisionQualityEngine.analyze("));
        assertFalse(metadataFactory.contains("ProceduralAutomationPolicyEngine.analyze("));
        assertFalse(metadataFactory.contains("ProceduralExecutiveExplainabilityService.analyze("));
        assertFalse(metadataFactory.contains("ProceduralAccelerationEngine.analyze("));

        assertTrue(seedFactory.contains("public Map<String, Object> build("));
        assertTrue(intelligenceResolver.contains("ProceduralIntelligenceAdvisor.analyzeRouting("));
        assertTrue(intelligenceResolver.contains("ProceduralDecisionQualityEngine.analyze("));
        assertTrue(intelligenceResolver.contains("ProceduralAutomationPolicyEngine.analyze("));
        assertTrue(intelligenceResolver.contains("ProceduralExecutiveExplainabilityService.analyze("));
        assertTrue(intelligenceResolver.contains("ProceduralAccelerationEngine.analyze("));
    }
}
