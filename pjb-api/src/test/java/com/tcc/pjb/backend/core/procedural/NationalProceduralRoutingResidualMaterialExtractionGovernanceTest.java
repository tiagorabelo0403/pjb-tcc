package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingResidualMaterialExtractionGovernanceTest {

    @Test
    void mustKeepResidualMaterialResolversOutOfMainServiceAndWithinCoreSubphases() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String trackClassificationResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingTrackClassificationResolver.java"));
        String probatoryResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralProbatoryProfileResolver.java"));
        String complexityResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralComplexityBandResolver.java"));
        String tipoJusticaResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralTipoJusticaResolver.java"));
        String distributionResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralDistributionResolver.java"));

        assertFalse(service.contains("probatoryProfileResolver.resolve("));
        assertFalse(service.contains("complexityBandResolver.resolve("));
        assertFalse(service.contains("tipoJusticaResolver.resolve("));
        assertFalse(service.contains("distributionResolver.resolve("));
        assertTrue(service.contains("coreAnalyzer.analyze("));

        assertTrue(trackClassificationResolver.contains("complexityBandResolver.resolve("));
        assertTrue(trackClassificationResolver.contains("tipoJusticaResolver.resolve("));
        assertFalse(trackClassificationResolver.contains("judicialPlacementResolver.resolve("));

        assertTrue(probatoryResolver.contains("String resolve("));
        assertTrue(complexityResolver.contains("String resolve("));
        assertTrue(tipoJusticaResolver.contains("TipoJustica resolve("));
        assertTrue(distributionResolver.contains("Optional<NationalProceduralDistributionSuggestion> resolve("));
    }
}
