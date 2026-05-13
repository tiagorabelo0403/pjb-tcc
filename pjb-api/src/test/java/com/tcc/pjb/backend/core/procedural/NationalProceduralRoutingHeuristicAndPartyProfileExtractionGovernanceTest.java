package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingHeuristicAndPartyProfileExtractionGovernanceTest {

    @Test
    void mustKeepHeuristicRitoAndPartyProfileExtractionOutOfMainService() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String heuristicResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralHeuristicRitoResolver.java"));
        String partyProfileResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralPartyProfileResolver.java"));

        assertFalse(service.contains("private RitoProcessual inferHeuristicRito("));
        assertFalse(service.contains("private NationalProceduralPartyProfile resolvePartyProfile("));

        assertTrue(heuristicResolver.contains("RitoProcessual resolve("));
        assertTrue(partyProfileResolver.contains("NationalProceduralPartyProfile resolve("));
    }
}
