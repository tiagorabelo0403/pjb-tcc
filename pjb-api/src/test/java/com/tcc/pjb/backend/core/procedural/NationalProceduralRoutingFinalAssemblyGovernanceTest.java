package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingFinalAssemblyGovernanceTest {

    @Test
    void mustKeepFinalAssemblyOutOfMainServiceAndInFinalizationStage() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String finalizer = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingFinalizationResolver.java"));
        String reportAssembler = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingReportAssembler.java"));

        assertFalse(service.contains("economicGateFactory.build("));
        assertFalse(service.contains("metadataFactory.build("));
        assertFalse(service.contains("routingReportAssembler.assemble("));
        assertTrue(service.contains("finalizationResolver.finalize("));

        assertTrue(finalizer.contains("economicGateFactory.build("));
        assertTrue(finalizer.contains("metadataContextFactory.create("));
        assertTrue(finalizer.contains("metadataFactory.build("));
        assertTrue(finalizer.contains("reportAssemblyContextFactory.create("));
        assertTrue(finalizer.contains("routingReportAssembler.assemble("));
        assertFalse(finalizer.contains("new NationalProceduralRoutingMetadataContext("));
        assertFalse(finalizer.contains("new NationalProceduralRoutingReportAssemblyContext("));
        assertTrue(reportAssembler.contains("ProceduralRoutingReport assemble("));
    }
}
