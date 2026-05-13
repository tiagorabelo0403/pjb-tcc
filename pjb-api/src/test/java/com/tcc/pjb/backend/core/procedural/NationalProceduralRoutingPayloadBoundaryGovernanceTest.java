package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingPayloadBoundaryGovernanceTest {

    @Test
    void mustKeepPayloadFactoryAsOrchestratorAndDelegateSecurityHardeningToDedicatedPolicy() throws Exception {
        String factory = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingPayloadFactory.java"));
        String policy = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingPayloadSecurityPolicy.java"));

        assertTrue(factory.contains("processoRequestPayloadAssembler.assemble("));
        assertTrue(factory.contains("laianePayloadAssembler.assemble("));
        assertTrue(factory.contains("processoEntityPayloadAssembler.assemble("));
        assertTrue(factory.contains("payloadSecurityPolicy.snapshot("));
        assertFalse(factory.contains("payload.put("));
        assertTrue(policy.contains("MAX_NESTED_DEPTH"));
        assertTrue(policy.contains("MAX_COLLECTION_ENTRIES"));
    }
}
