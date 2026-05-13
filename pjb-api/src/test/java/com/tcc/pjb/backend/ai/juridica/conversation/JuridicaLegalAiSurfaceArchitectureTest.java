package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;

class JuridicaLegalAiSurfaceArchitectureTest {

@Test
    void structuredLegalSurfacesMustReuseSovereignPromotionGovernance() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("service/intelligence/surface/LegalAiStructuredSurfaceGovernanceService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/surface/LegalAiStructuredSurfaceGovernanceSnapshot.java")));
        String facade = Files.readString(root.resolve("service/intelligence/surface/LegalAiSurfaceFacadeService.java"));
        String draftResponse = Files.readString(root.resolve("model/dto/ai/legal/LegalDraftResponse.java"));
        String guardResponse = Files.readString(root.resolve("model/dto/ai/legal/LegalHallucinationGuardResponse.java"));
        assertTrue(facade.contains("surfacePromotionGovernanceService"));
        assertTrue(facade.contains("toDraftConversationRequest"));
        assertTrue(facade.contains("toGroundingConversationRequest"));
        assertTrue(facade.contains("governedDraftScaffold"));
        assertTrue(facade.contains("groundingPromotionStatus"));
        assertTrue(draftResponse.contains("safeguards"));
        assertTrue(guardResponse.contains("evidenceProvenanceStatus"));
        assertTrue(guardResponse.contains("groundingPromotionStatus"));
    }

@Test
    void sovereignEvidenceRegistryMustBeConnectedToConversationAndStructuredSurface() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalEvidenceSovereignRegistryService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationEvidenceDescriptor.java")));
        String provenanceService = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationEvidenceProvenanceService.java"));
        String provenanceSnapshot = Files.readString(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationEvidenceProvenanceSnapshot.java"));
        String surfaceFacade = Files.readString(root.resolve("service/intelligence/surface/LegalAiSurfaceFacadeService.java"));
        assertTrue(provenanceService.contains("LegalEvidenceSovereignRegistryService"));
        assertTrue(provenanceService.contains("evidenceRegistry.descriptors()"));
        assertTrue(provenanceSnapshot.contains("promotedGroundingEvidenceIds"));
        assertTrue(provenanceSnapshot.contains("evidenceDescriptors"));
        assertTrue(surfaceFacade.contains("Evidências promovidas para minuta"));
        assertTrue(surfaceFacade.contains("Descritores soberanos de evidência"));
    }

@Test
    void structuredLegalSurfacesMustAssembleOnlyPromotedEvidenceBeforePromptOrGrounding() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("service/intelligence/surface/LegalAiStructuredSurfaceEvidenceAssemblerService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/surface/LegalAiStructuredSurfaceEvidenceBundle.java")));
        String facade = Files.readString(root.resolve("service/intelligence/surface/LegalAiSurfaceFacadeService.java"));
        String assembler = Files.readString(root.resolve("service/intelligence/surface/LegalAiStructuredSurfaceEvidenceAssemblerService.java"));
        assertTrue(facade.contains("surfaceEvidenceAssemblerService"));
        assertTrue(facade.contains("assembleForDraft(governance)"));
        assertTrue(facade.contains("assembleForGrounding(governance)"));
        assertTrue(facade.contains("promotedEvidenceDescriptors"));
        assertTrue(assembler.contains("promotedForDraft"));
        assertTrue(assembler.contains("promotedForGrounding"));
        String commentaryCatalog = Files.readString(root.resolve("ai/juridica/knowledge/support/LegalKnowledgeCommentaryTextCatalogService.java"));
        assertTrue(commentaryCatalog.contains("SURFACE_PROMOTION_NOT_ANCHORED"));
    }

@Test
    void legalAiSurfacesMustHaveContractsItsAndSpecificRouteGovernance() throws Exception {
        Path testRoot = PjbTestPaths.backendTestRoot();
        Path resourceRoot = PjbTestPaths.pjbApiTestResourcesRoot().resolve("pacts/provider");
        Path configRoot = PjbTestPaths.pjbApiMainResourcesRoot().resolve("application.yml");
        assertTrue(Files.exists(testRoot.resolve("contracts/provider/LegalAiControllerProviderContractTest.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiControllerIT.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiConversationControllerIT.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiKnowledgeControllerIT.java")));
        assertTrue(Files.exists(resourceRoot.resolve("PjbLegalAiConsumer-PjbLegalAiProvider.json")));
        Path scriptsRoot = PjbTestPaths.projectRoot().resolve("scripts/legal_ai_surface_split_guard.py");
        assertTrue(Files.exists(scriptsRoot));
        String yaml = Files.readString(configRoot);
        assertTrue(yaml.contains("name: legal-ai-governed-surfaces"));
        assertTrue(yaml.contains("/api/ai/legal/minuta"));
        assertTrue(yaml.contains("/api/ai/legal/grounding/check"));
        assertTrue(yaml.contains("/api/ai/legal/conversation"));
        assertTrue(yaml.contains("PJB_API_ROUTE_GOVERNANCE_LEGAL_AI_SURFACE_RATE_LIMIT"));
    }
}
