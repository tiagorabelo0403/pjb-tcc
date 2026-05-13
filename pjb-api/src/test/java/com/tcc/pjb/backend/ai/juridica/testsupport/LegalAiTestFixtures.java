package com.tcc.pjb.backend.ai.juridica.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.tcc.pjb.backend.ai.common.VectorSearchServiceMock;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationApprovalService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationCapabilityCooldownService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationCapabilityRecurrenceService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationCapabilityRecoveryService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationCapabilityRehabilitationService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationCapabilitySuppressionService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationContextAssemblerService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationMemoryService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationOrchestrator;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiJuridicalLineageRegistry;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiPreConsciousFrameService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiPreConsciousSignalExtractor;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiPreConsciousToolScopeEnricher;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationResponseComposerService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationRoutingService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationSessionBootstrapService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationSessionDoctorService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationTraceService;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationTrustZoneService;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaVirtualTrendsCouncilService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAuditTrailService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDocumentQuarantineService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSensitiveActionApprovalService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSourceAllowlist;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalToolScopePolicy;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaHallucinationGuardService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaResearchDossierService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaValidationEnvelopeService;
import com.tcc.pjb.backend.ai.juridica.symbolic.JuridicaSymbolicValidationExecutionService;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.ai.juridica.conversation.LegalAiConversationEvidenceProvenanceService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAttachmentProvenanceClassifier;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDraftPromotionFence;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidencePromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceSovereignRegistryService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceTrustClassifier;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalGroundingPromotionFence;
import com.tcc.pjb.backend.ai.juridica.eval.LegalBenchmarkCatalog;
import com.tcc.pjb.backend.ai.juridica.eval.LegalEvalReplayRunner;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpPlanScorer;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerDemotionPolicy;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerPromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.knowledge.LegalKnowledgeCoverageService;
import com.tcc.pjb.backend.ai.juridica.knowledge.LegalKnowledgeSourceCatalogService;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeJsonResourceLoader;
import com.tcc.pjb.backend.ai.juridica.mcp.JuridicaMcpServerCatalogService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalAgendaPrazosMcpServer;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalDocumentalMcpServer;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalInteroperabilityMcpServer;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalJurisprudenceMcpServer;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalLegislationMcpServer;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpContextCompactionService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpDeliberationCheckpointService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpDoctorService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpEvidencePromotionService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpExecutionTranscriptService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpSkillCatalogService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpToolExampleRegistry;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalProcessualMcpServer;
import com.tcc.pjb.backend.ai.juridica.mcp.support.LegalMcpTextCatalogService;
import com.tcc.pjb.backend.ai.juridica.mcp.support.LegalMcpToolExampleCatalogService;
import com.tcc.pjb.backend.ai.juridica.policy.JuridicaMcpRagFusionService;
import com.tcc.pjb.backend.ai.juridica.policy.JuridicaStrategicExecutionService;
import com.tcc.pjb.backend.ai.juridica.policy.support.LegalAiPolicyTextCatalogService;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCoverageSnapshot;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;

public final class LegalAiTestFixtures {

    private LegalAiTestFixtures() {
    }

    public static LegalKnowledgeJsonResourceLoader knowledgeJsonResourceLoader() {
        return new LegalKnowledgeJsonResourceLoader(new ObjectMapper().findAndRegisterModules());
    }

    public static LegalMcpTextCatalogService mcpTextCatalogService() {
        return new LegalMcpTextCatalogService(knowledgeJsonResourceLoader());
    }

    public static LegalMcpToolExampleRegistry toolExampleRegistry() {
        return new LegalMcpToolExampleRegistry(new LegalMcpToolExampleCatalogService(knowledgeJsonResourceLoader()));
    }

    public static LegalMcpDeliberationCheckpointService deliberationCheckpointService() {
        return new LegalMcpDeliberationCheckpointService(mcpTextCatalogService());
    }

    public static LegalAiPolicyTextCatalogService policyTextCatalogService() {
        return new LegalAiPolicyTextCatalogService(knowledgeJsonResourceLoader());
    }

    public static JuridicaMcpRagFusionService ragFusionService() {
        return new JuridicaMcpRagFusionService(policyTextCatalogService());
    }

    public static JuridicaStrategicExecutionService strategicExecutionService() {
        return new JuridicaStrategicExecutionService(policyTextCatalogService());
    }

    public static JuridicaMcpServerCatalogService mcpCatalog() {
        return new JuridicaMcpServerCatalogService(
                List.of(
                        new LegalLegislationMcpServer(),
                        new LegalJurisprudenceMcpServer(),
                        new LegalProcessualMcpServer(),
                        new LegalDocumentalMcpServer(),
                        new LegalAgendaPrazosMcpServer(),
                        new LegalInteroperabilityMcpServer()
                ),
                new LegalAiStructuredSchemaCatalog(),
                new LegalEvalReplayRunner(
                        new LegalBenchmarkCatalog(),
                        new LegalMcpPlanScorer(),
                        new LegalMcpServerPromotionPolicy(),
                        new LegalMcpServerDemotionPolicy()
                ),
                new LegalMcpSkillCatalogService(),
                toolExampleRegistry(),
                deliberationCheckpointService(),
                new LegalMcpContextCompactionService(),
                new LegalMcpExecutionTranscriptService(),
                new LegalMcpDoctorService(),
                new LegalMcpEvidencePromotionService(),
                mcpTextCatalogService()
        );
    }

    public static LegalKnowledgeSourceCatalogService knowledgeSourceCatalogService() {
        LegalKnowledgeSourceCatalogService service = new LegalKnowledgeSourceCatalogService(knowledgeJsonResourceLoader());
        invokeNoArg(service, "load");
        return service;
    }

    public static LegalKnowledgeCoverageSnapshot coverageSnapshot() {
        LegalKnowledgeSourceCatalogService catalog = knowledgeSourceCatalogService();
        return new LegalKnowledgeCoverageSnapshot(
                "OFFICIAL_READY",
                "OFFICIAL_PRIMARY_ONLY",
                List.of("CIVIL"),
                catalog.listOfficialSources().stream().limit(2).toList(),
                List.of(),
                catalog.priorityOrder(),
                catalog.ingestionPolicies(),
                List.of("TEST_COVERAGE_READY"),
                Map.of("fixture", "LegalAiTestFixtures")
        );
    }

    public static LegalKnowledgeCoverageService coverageServiceStub() {
        LegalKnowledgeCoverageService service = Mockito.mock(LegalKnowledgeCoverageService.class);
        Mockito.when(service.inspect(Mockito.any(LegalAiConversationRequest.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(coverageSnapshot());
        return service;
    }

    public static LegalAiConversationEvidenceProvenanceService evidenceProvenanceService() {
        return new LegalAiConversationEvidenceProvenanceService(
                new LegalEvidenceTrustClassifier(),
                new LegalAttachmentProvenanceClassifier(),
                new LegalEvidencePromotionPolicy(
                        new LegalGroundingPromotionFence(),
                        new LegalDraftPromotionFence()
                ),
                new LegalEvidenceSovereignRegistryService()
        );
    }


    public static LegalAiConversationOrchestrator conversationOrchestrator(
            JuridicaUnifiedMeshProfileService mesh,
            JuridicaLegalAiSpineService spine,
            LegalAiStructuredSchemaCatalog catalog
    ) {
        return new LegalAiConversationOrchestrator(
                new LegalAiConversationRoutingService(),
                new LegalAiConversationContextAssemblerService(mesh, spine, catalog),
                new JuridicaResearchDossierService(new VectorSearchServiceMock(), mesh, spine, catalog),
                new JuridicaValidationEnvelopeService(spine, symbolicExecutionService()),
                new JuridicaHallucinationGuardService(spine),
                new JuridicaVirtualTrendsCouncilService(),
                new LegalAiConversationResponseComposerService(),
                new LegalAiConversationMemoryService(),
                new LegalAiConversationTraceService(new LegalAuditTrailService()),
                new LegalAiConversationApprovalService(new LegalSensitiveActionApprovalService()),
                new LegalContextSanitizer(),
                new LegalSourceAllowlist(),
                new LegalDocumentQuarantineService(),
                new LegalToolScopePolicy(),
                new LegalAiConversationSessionDoctorService(),
                new LegalAiConversationSessionBootstrapService(),
                new LegalAiConversationCapabilityRecoveryService(),
                new LegalAiConversationCapabilityCooldownService(),
                new LegalAiConversationCapabilityRehabilitationService(),
                new LegalAiConversationCapabilityRecurrenceService(),
                new LegalAiConversationCapabilitySuppressionService(),
                new LegalAiConversationTrustZoneService(),
                evidenceProvenanceService(),
                coverageServiceStub(),
                new LegalAiPreConsciousFrameService(new LegalAiJuridicalLineageRegistry(), new LegalAiPreConsciousSignalExtractor()),
                new LegalAiPreConsciousToolScopeEnricher()
        );
    }

    public static JuridicaSymbolicValidationExecutionService symbolicExecutionService() {
        java.util.List<com.tcc.pjb.backend.ai.juridica.symbolic.LegalDeterministicRuleEngine> engines = java.util.List.of(
                new com.tcc.pjb.backend.ai.juridica.symbolic.LegalCabimentoRuleEngine(),
                new com.tcc.pjb.backend.ai.juridica.symbolic.LegalCompetenciaRuleEngine(),
                new com.tcc.pjb.backend.ai.juridica.symbolic.LegalPrazoRuleEngine(),
                new com.tcc.pjb.backend.ai.juridica.symbolic.LegalSigiloRuleEngine(),
                new com.tcc.pjb.backend.ai.juridica.symbolic.LegalProceduralCompatibilityEngine()
        );
        return new JuridicaSymbolicValidationExecutionService(engines);
    }

    public static LegalMcpServerDescriptor serverDescriptor(String id) {
        return new LegalMcpServerDescriptor(
                id,
                id,
                "LEGAL",
                "STREAMABLE_HTTP",
                "OPTIONAL",
                true,
                true,
                true,
                false,
                "READONLY",
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    public static LegalMcpExecutionPlan executionPlan(
            String planId,
            String selectionMode,
            boolean discoveryEnabled,
            List<LegalMcpServerDescriptor> pinnedServers,
            List<LegalMcpServerDescriptor> fallbackServers,
            List<String> reasons,
            List<String> safeguards
    ) {
        return new LegalMcpExecutionPlan(
                planId,
                selectionMode,
                discoveryEnabled,
                "JSON_RPC_BATCH_SAFE_READONLY",
                "OAUTH2_1_GOVERNED",
                "JSON_RPC_BATCH_SAFE_READONLY",
                "ARGUMENT_COMPLETIONS_AND_PINNED_PROMPTS",
                "SIGNED_CONTEXT_AND_AUTHORITY_FROZEN",
                5,
                3,
                pinnedServers,
                fallbackServers,
                List.of("A"),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                reasons,
                safeguards,
                null
        );
    }

    private static void invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not invoke " + methodName, e);
        }
    }
}
