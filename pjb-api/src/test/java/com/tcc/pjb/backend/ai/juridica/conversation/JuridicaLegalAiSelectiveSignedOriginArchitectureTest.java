package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;

class JuridicaLegalAiSelectiveSignedOriginArchitectureTest {

    @Test
    void legalAiConversationMustHaveSelectiveSignedOriginEvidenceOnBoundary() throws Exception {
        Path testRoot = PjbTestPaths.backendTestRoot();
        Path resourceRoot = PjbTestPaths.pjbApiTestResourcesRoot().resolve("pacts/provider");
        Path configRoot = PjbTestPaths.pjbApiMainResourcesRoot().resolve("application.yml");
        assertTrue(Files.exists(testRoot.resolve("contracts/provider/LegalAiSelectiveSignedOriginProviderContractTest.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiSelectiveSignedOriginGovernanceIT.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiSignedOriginTestSupport.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/conversation/JuridicaLegalAiSelectiveSignedOriginArchitectureTest.java")));
        assertTrue(Files.exists(resourceRoot.resolve("PjbLegalAiSelectiveSignedOriginConsumer-PjbLegalAiSelectiveSignedOriginProvider.json")));
        String pact = Files.readString(resourceRoot.resolve("PjbLegalAiSelectiveSignedOriginConsumer-PjbLegalAiSelectiveSignedOriginProvider.json"));
        assertTrue(pact.contains("LEGAL_GENERAL_ASSIST_V3"));
        assertTrue(pact.contains("LEGAL_DRAFT_V2"));
        assertTrue(pact.contains("signed_attestation_required_for_capability"));
        assertTrue(pact.contains("X-PJB-Origin-Requirement"));
        assertTrue(pact.contains("X-PJB-Origin-Capability"));
        String yaml = Files.readString(configRoot);
        assertTrue(yaml.contains("selective-signed-rules"));
        assertTrue(yaml.contains("legal-ai-conversation-sensitive-capabilities"));
        assertTrue(yaml.contains("X-PJB-Origin-Requirement"));
        assertTrue(yaml.contains("X-PJB-Origin-Capability"));
    }
}
