package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfilePrivateRightsSubphasesGovernanceTest {

    @Test
    void mustKeepPrivateRightsResolverAsOrchestratorBetweenFamilyPropertyBusinessAndConsumerLanes() throws Exception {
        String privateRightsResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfilePrivateRightsResolver.java"));
        String familyResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileFamilyResolver.java"));
        String propertyResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfilePropertyResolver.java"));
        String businessResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileBusinessResolver.java"));
        String consumerResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileConsumerResolver.java"));

        assertTrue(privateRightsResolver.contains("familyResolver.resolve(context)"));
        assertTrue(privateRightsResolver.contains("propertyResolver.resolve(context)"));
        assertTrue(privateRightsResolver.contains("businessResolver.resolve(context)"));
        assertTrue(privateRightsResolver.contains("consumerResolver.resolve(context)"));
        assertFalse(privateRightsResolver.contains("USUCAPIAO"));
        assertFalse(privateRightsResolver.contains("RECUPERACAO JUDICIAL"));
        assertFalse(privateRightsResolver.contains("CONSUMIDOR"));

        assertTrue(familyResolver.contains("INVENTARIO"));
        assertTrue(propertyResolver.contains("USUCAPIAO"));
        assertTrue(businessResolver.contains("RECUPERACAO JUDICIAL"));
        assertTrue(consumerResolver.contains("CONSUMIDOR"));
    }
}
