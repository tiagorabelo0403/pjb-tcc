package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;

class JuridicaLegalAiConversationArchitectureTest {

@Test
    void conversationMustBeMountedInsideExistingLegalAiAndExposeFiveVirtualTrends() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/JuridicaLegalAiConversationService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/JuridicaVirtualTrendsCouncilService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/JuridicaVirtualTrendsCatalog.java")));
        String controller = Files.readString(root.resolve("ai/juridica/api/LegalAiConversationController.java"));
        String facade = Files.readString(root.resolve("service/intelligence/surface/LegalAiSurfaceFacadeService.java"));
        String council = Files.readString(root.resolve("ai/juridica/conversation/JuridicaVirtualTrendsCouncilService.java"));
        String catalog = Files.readString(root.resolve("ai/juridica/conversation/JuridicaVirtualTrendsCatalog.java"));
        assertTrue(controller.contains("/conversation"));
        assertTrue(facade.contains("converse("));
        assertTrue(council.contains("JuridicaVirtualTrendsCatalog.profiles()"));
        assertTrue(catalog.contains("RELATOR_ESTRUTURAL"));
        assertTrue(catalog.contains("LEGISLADOR_NORMATIVO"));
        assertTrue(catalog.contains("PRECEDENTES_ESTRATEGICOS"));
        assertTrue(catalog.contains("AUDITOR_SIMBOLICO"));
        assertTrue(catalog.contains("REDATOR_CONVERSACIONAL"));
    }

@Test
    void conversationMustUseDedicatedOrchestratorMemoryTraceAndApprovalServices() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationMemoryService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationTraceService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationApprovalService.java")));
        String service = Files.readString(root.resolve("ai/juridica/conversation/JuridicaLegalAiConversationService.java"));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String assembler = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationContextAssemblerService.java"));
        assertTrue(service.contains("LegalAiConversationOrchestrator"));
        assertTrue(orchestrator.contains("conversationMemory"));
        assertTrue(orchestrator.contains("conversationTrace"));
        assertTrue(orchestrator.contains("conversationApproval"));
        assertTrue(assembler.contains("juridicaConversationMemory"));
        assertTrue(assembler.contains("juridicaConversationTrace"));
        assertTrue(assembler.contains("juridicaConversationApproval"));
    }

@Test
    void conversationMustUseDocumentSecurityAndPromptInjectionHardeningServices() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalContextSanitizer.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalSourceAllowlist.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalDocumentQuarantineService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalAuditTrailService.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String assembler = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationContextAssemblerService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("conversationDocumentSecurity"));
        assertTrue(orchestrator.contains("conversationToolScope"));
        assertTrue(orchestrator.contains("conversationSanitization"));
        assertTrue(assembler.contains("juridicaConversationDocumentSecurity"));
        assertTrue(assembler.contains("juridicaConversationToolScope"));
        assertTrue(assembler.contains("juridicaConversationSanitization"));
        assertTrue(composer.contains("documentSecurityStatus"));
        assertTrue(composer.contains("promptInjectionDetected"));
    }

@Test
    void conversationMustWireSessionDoctorToToolScopeApprovalAndSafeguards() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationSessionDoctorService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationSessionDoctorSnapshot.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("sessionDoctorService"));
        assertTrue(orchestrator.contains("conversationSessionDoctor"));
        assertTrue(toolScope.contains("enrichWithSessionDoctor"));
        assertTrue(approval.contains("SESSION_DOCTOR_"));
        assertTrue(composer.contains("sessionDoctorStatus"));
        assertTrue(composer.contains("sessionDoctorBlockedToolExampleIds"));
    }

@Test
    void conversationMustWireSessionBootstrapToToolScopeApprovalAndSafeguards() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationSessionBootstrapService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationSessionBootstrapSnapshot.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("sessionBootstrapService"));
        assertTrue(orchestrator.contains("conversationSessionBootstrap"));
        assertTrue(toolScope.contains("enrichWithSessionBootstrap"));
        assertTrue(approval.contains("SESSION_BOOTSTRAP_"));
        assertTrue(composer.contains("sessionBootstrapStatus"));
        assertTrue(composer.contains("sessionBootstrapMissingToolExampleIds"));
    }

@Test
    void conversationMustWireCapabilityRecoveryAcrossOrchestratorToolScopeApprovalAndSafeguards() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationCapabilityRecoveryService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationCapabilityRecoverySnapshot.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("capabilityRecoveryService"));
        assertTrue(orchestrator.contains("conversationCapabilityRecovery"));
        assertTrue(toolScope.contains("enrichWithCapabilityRecovery"));
        assertTrue(toolScope.contains("sessionBootstrapRecoveryCandidateToolIds"));
        assertTrue(approval.contains("CAPABILITY_RECOVERY_"));
        assertTrue(composer.contains("capabilityRecoveryStatus"));
        assertTrue(composer.contains("capabilityRecoveryCandidateToolIds"));
    }

@Test
    void conversationMustWireCapabilityCooldownAcrossOrchestratorToolScopeApprovalAndSafeguards() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationCapabilityCooldownService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationCapabilityCooldownSnapshot.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("capabilityCooldownService"));
        assertTrue(orchestrator.contains("conversationCapabilityCooldown"));
        assertTrue(toolScope.contains("enrichWithCapabilityCooldown"));
        assertTrue(approval.contains("CAPABILITY_COOLDOWN_"));
        assertTrue(composer.contains("capabilityCooldownStatus"));
        assertTrue(composer.contains("capabilityCooldownTurnsRemaining"));
    }

@Test
    void conversationMustWireCapabilityRehabilitationAcrossOrchestratorToolScopeApprovalAndSafeguards() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationCapabilityRehabilitationService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationCapabilityRehabilitationSnapshot.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("capabilityRehabilitationService"));
        assertTrue(orchestrator.contains("conversationCapabilityRehabilitation"));
        assertTrue(toolScope.contains("enrichWithCapabilityRehabilitation"));
        assertTrue(approval.contains("CAPABILITY_REHABILITATION_"));
        assertTrue(composer.contains("capabilityRehabilitationStatus"));
        assertTrue(composer.contains("capabilityRehabilitationWindowTurnsRemaining"));
    }

@Test
    void conversationMustWireCapabilityRecurrenceAcrossOrchestratorToolScopeApprovalAndSafeguards() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationCapabilityRecurrenceService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationCapabilityRecurrenceSnapshot.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("capabilityRecurrenceService"));
        assertTrue(orchestrator.contains("conversationCapabilityRecurrence"));
        assertTrue(toolScope.contains("enrichWithCapabilityRecurrence"));
        assertTrue(approval.contains("CAPABILITY_RECURRENCE_"));
        assertTrue(composer.contains("capabilityRecurrenceStatus"));
        assertTrue(composer.contains("capabilityRecurrenceRiskTier"));
    }

@Test
    void conversationMustWireCapabilitySuppressionAcrossOrchestratorToolScopeApprovalAndSafeguards() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationCapabilitySuppressionService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationCapabilitySuppressionSnapshot.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("capabilitySuppressionService"));
        assertTrue(orchestrator.contains("conversationCapabilitySuppression"));
        assertTrue(toolScope.contains("enrichWithCapabilitySuppression"));
        assertTrue(approval.contains("CAPABILITY_SUPPRESSION_"));
        assertTrue(composer.contains("capabilitySuppressionStatus"));
        assertTrue(composer.contains("capabilitySuppressionPolicyTier"));
    }

@Test
    void conversationMustWireTrustZoneAcrossOrchestratorToolScopeApprovalAndSafeguards() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationTrustZoneService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationTrustZoneSnapshot.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("trustZoneService"));
        assertTrue(orchestrator.contains("conversationTrustZone"));
        assertTrue(toolScope.contains("enrichWithTrustZone"));
        assertTrue(approval.contains("TRUST_ZONE_"));
        assertTrue(composer.contains("trustZoneStatus"));
        assertTrue(composer.contains("trustZoneMode"));
    }

@Test
    void conversationMustWireEvidenceProvenanceAcrossOrchestratorToolScopeApprovalAndComposer() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/LegalAiConversationEvidenceProvenanceService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/conversation/LegalAiConversationEvidenceProvenanceSnapshot.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalEvidencePromotionPolicy.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalEvidenceTrustClassifier.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalAttachmentProvenanceClassifier.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalGroundingPromotionFence.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/conversation/security/LegalDraftPromotionFence.java")));
        String orchestrator = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationOrchestrator.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(orchestrator.contains("evidenceProvenanceService"));
        assertTrue(orchestrator.contains("conversationEvidenceProvenance"));
        assertTrue(toolScope.contains("enrichWithEvidenceProvenance"));
        assertTrue(approval.contains("EVIDENCE_PROVENANCE_"));
        assertTrue(composer.contains("evidenceProvenanceStatus"));
        assertTrue(composer.contains("draftPromotionStatus"));
    }

@Test
    void conversationBoundaryMustHaveContractItsAndSpecificRouteGovernance() throws Exception {
        Path testRoot = PjbTestPaths.backendTestRoot();
        Path resourceRoot = PjbTestPaths.pjbApiTestResourcesRoot().resolve("pacts/provider");
        Path configRoot = PjbTestPaths.pjbApiMainResourcesRoot().resolve("application.yml");
        assertTrue(Files.exists(testRoot.resolve("contracts/provider/LegalAiControllerProviderContractTest.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiControllerIT.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiConversationControllerIT.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/api/LegalAiKnowledgeControllerIT.java")));
        assertTrue(Files.exists(testRoot.resolve("ai/juridica/conversation/JuridicaLegalAiConversationArchitectureTest.java")));
        assertTrue(Files.exists(resourceRoot.resolve("PjbLegalAiConsumer-PjbLegalAiProvider.json")));
        String pact = Files.readString(resourceRoot.resolve("PjbLegalAiConsumer-PjbLegalAiProvider.json"));
        assertTrue(pact.contains("/api/ai/legal/conversation"));
        assertTrue(pact.contains("approvalStatus"));
        assertTrue(pact.contains("trustZoneStatus"));
        assertTrue(pact.contains("evidenceProvenanceTier"));
        Path scriptsRoot = PjbTestPaths.projectRoot().resolve("scripts/legal_ai_surface_split_guard.py");
        assertTrue(Files.exists(scriptsRoot));
        String yaml = Files.readString(configRoot);
        assertTrue(yaml.contains("name: legal-ai-governed-surfaces"));
        assertTrue(yaml.contains("/api/ai/legal/conversation"));
        assertTrue(yaml.contains("PJB_API_ROUTE_GOVERNANCE_LEGAL_AI_SURFACE_RATE_LIMIT"));
    }
}
