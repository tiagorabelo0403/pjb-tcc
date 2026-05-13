package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingForumAllocationBoundaryGovernanceTest {

    @Test
    void mustKeepForumAllocationSplitIntoSeedReadinessAndAssemblyBoundaries() throws Exception {
        String resolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationResolver.java"));
        String seedResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationSeedResolver.java"));
        String readinessResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumRoutingReadinessResolver.java"));
        String assembler = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationReportAssembler.java"));

        assertTrue(resolver.contains("seedResolver.resolve("));
        assertTrue(resolver.contains("readinessResolver.resolve("));
        assertTrue(resolver.contains("reportAssembler.assemble("));
        assertFalse(resolver.contains("resolveFallbackPerfil("));
        assertFalse(resolver.contains("new ProceduralForumAllocationReport("));
        assertTrue(seedResolver.contains("classSeedResolver.resolve("));
        assertTrue(seedResolver.contains("baseSeedResolver.resolve("));
        assertTrue(seedResolver.contains("profileResolver.resolve("));
        assertFalse(seedResolver.contains("varasDisponiveisNaComarca("));
        assertTrue(readinessResolver.contains("tribunalProtocolRoutingService.resolve("));
        assertTrue(readinessResolver.contains("proceduralPreflightEngine.evaluate("));
        assertTrue(assembler.contains("PayloadMaps.deepCopyWithoutNulls("));
    }
}
