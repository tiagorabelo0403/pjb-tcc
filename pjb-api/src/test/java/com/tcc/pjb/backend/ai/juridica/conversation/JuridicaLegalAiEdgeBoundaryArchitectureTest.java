package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;

class JuridicaLegalAiEdgeBoundaryArchitectureTest {

    @Test
    void legalAiBoundariesMustHaveEdgeContractsAndItsForOriginAndContentTypeRejection() throws Exception {
        Path testRoot = PjbTestPaths.backendTestRoot();
        Path resourceRoot = PjbTestPaths.pjbApiTestResourcesRoot().resolve("pacts/provider");
        Path configRoot = PjbTestPaths.pjbApiMainResourcesRoot().resolve("application.yml");
        assertTrue(Files.exists(testRoot.resolve("contracts/provider/LegalAiEdgePolicyProviderContractTest.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiEdgeGovernanceIT.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/conversation/JuridicaLegalAiEdgeBoundaryArchitectureTest.java")));
        assertTrue(Files.exists(resourceRoot.resolve("PjbLegalAiEdgePolicyConsumer-PjbLegalAiEdgePolicyProvider.json")));
        String pact = Files.readString(resourceRoot.resolve("PjbLegalAiEdgePolicyConsumer-PjbLegalAiEdgePolicyProvider.json"));
        assertTrue(pact.contains("browser_origin_not_allowed"));
        assertTrue(pact.contains("content_type_not_allowed"));
        assertTrue(pact.contains("/api/ai/legal/minuta"));
        assertTrue(pact.contains("/api/ai/legal/grounding/check"));
        assertTrue(pact.contains("/api/ai/legal/conversation"));
        String yaml = Files.readString(configRoot)
                + Files.readString(configRoot.resolveSibling("application-security.yml"))
                + Files.readString(configRoot.resolveSibling("application-api-governance.yml"));
        assertTrue(yaml.contains("name: legal-ai-governed-surfaces"));
        assertTrue(yaml.contains("/api/ai/legal/minuta"));
        assertTrue(yaml.contains("/api/ai/legal/grounding/check"));
        assertTrue(yaml.contains("/api/ai/legal/conversation"));
        assertTrue(yaml.contains("origin-governance"));
    }
}
