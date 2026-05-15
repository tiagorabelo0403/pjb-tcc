package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;

class JuridicaLegalAiSignedOriginArchitectureTest {

    @Test
    void legalAiBoundariesMustHaveSignedOriginItsContractsAndHashFirstEvidence() throws Exception {
        Path testRoot = PjbTestPaths.backendTestRoot();
        Path resourceRoot = PjbTestPaths.pjbApiTestResourcesRoot().resolve("pacts/provider");
        Path configRoot = PjbTestPaths.pjbApiMainResourcesRoot().resolve("application.yml");
        assertTrue(Files.exists(testRoot.resolve("contracts/provider/LegalAiSignedOriginProviderContractTest.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiSignedOriginGovernanceIT.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiSignedOriginTestSupport.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/conversation/JuridicaLegalAiSignedOriginArchitectureTest.java")));
        assertTrue(Files.exists(resourceRoot.resolve("PjbLegalAiSignedOriginConsumer-PjbLegalAiSignedOriginProvider.json")));
        String pact = Files.readString(resourceRoot.resolve("PjbLegalAiSignedOriginConsumer-PjbLegalAiSignedOriginProvider.json"));
        assertTrue(pact.contains("X-PJB-Origin-Id"));
        assertTrue(pact.contains("X-PJB-Body-Hash"));
        assertTrue(pact.contains("X-PJB-Timestamp"));
        assertTrue(pact.contains("X-PJB-Signature"));
        assertTrue(pact.contains("SIGNED_ATTESTATION"));
        assertTrue(pact.contains("BODY_HASH_MISMATCH"));
        assertTrue(pact.contains("signed_origin_signature_invalid"));
        assertTrue(pact.contains("signed_origin_id_required"));
        assertTrue(pact.contains("/api/ai/legal/minuta"));
        assertTrue(pact.contains("/api/ai/legal/grounding/check"));
        assertTrue(pact.contains("/api/ai/legal/conversation"));
        String yaml = Files.readString(configRoot)
                + Files.readString(configRoot.resolveSibling("application-security.yml"));
        assertTrue(yaml.contains("X-PJB-Origin-Id"));
        assertTrue(yaml.contains("X-PJB-Signature-Alg"));
        assertTrue(yaml.contains("X-PJB-Origin-Mode"));
        assertTrue(yaml.contains("X-PJB-Origin-Subject"));
    }
}
