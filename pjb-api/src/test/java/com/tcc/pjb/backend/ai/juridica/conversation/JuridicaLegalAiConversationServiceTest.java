package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.ai.common.VectorSearchServiceMock;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAuditTrailService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDocumentQuarantineService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSensitiveActionApprovalService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSourceAllowlist;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalToolScopePolicy;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaLegalToolCatalogService;
import com.tcc.pjb.backend.ai.juridica.eval.LegalBenchmarkCatalog;
import com.tcc.pjb.backend.ai.juridica.eval.LegalEvalReplayRunner;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpPlanScorer;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerDemotionPolicy;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerPromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.mcp.JuridicaMcpServerCatalogService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpToolExampleRegistry;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpSkillCatalogService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpDeliberationCheckpointService;
import com.tcc.pjb.backend.ai.juridica.mcp.LegalMcpContextCompactionService;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaAntiHallucinationProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaEvaluationProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaGraphProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaHallucinationGuardService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaHybridRetrievalProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaMemoryIsolationProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaMultimodalProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaPolicyVariableService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaResearchDossierService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaStructuredOutputProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaSymbolicValidationProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaToolRoutingService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaTraceApprovalService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaValidationEnvelopeService;
import com.tcc.pjb.backend.ai.juridica.symbolic.JuridicaSymbolicValidationExecutionService;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalCabimentoRuleEngine;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalCompetenciaRuleEngine;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalPrazoRuleEngine;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalProceduralCompatibilityEngine;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalSigiloRuleEngine;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JuridicaLegalAiConversationServiceTest {

    private static JuridicaMcpServerCatalogService mcpCatalog() {
        return com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.mcpCatalog();
    }


    @Test
    void mustAssembleConversationWithFiveVirtualTrendsAndDocumentSecurity() {
        var catalog = new LegalAiStructuredSchemaCatalog();
        var mesh = new JuridicaUnifiedMeshProfileService(new JuridicaLegalToolCatalogService(), mcpCatalog(), new LegalAiStructuredSchemaCatalog());
        var spine = new JuridicaLegalAiSpineService(
                new JuridicaPolicyVariableService(),
                new JuridicaToolRoutingService(new JuridicaLegalToolCatalogService()),
                new JuridicaStructuredOutputProfileService(catalog),
                new JuridicaHybridRetrievalProfileService(),
                new JuridicaMemoryIsolationProfileService(),
                new JuridicaSymbolicValidationProfileService(),
                new JuridicaGraphProfileService(),
                new JuridicaMultimodalProfileService(),
                new JuridicaEvaluationProfileService(),
                new JuridicaAntiHallucinationProfileService(),
                new JuridicaTraceApprovalService()
        );
        var orchestrator = com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.conversationOrchestrator(mesh, spine, catalog);
        var service = new JuridicaLegalAiConversationService(orchestrator);

        var response = service.converse(new LegalAiConversationRequest(
                "conv-1",
                "12345",
                "Quais precedentes do STJ e do STF podem incidir neste caso e qual o prazo recursal?",
                "ADVOGADO",
                List.of("Contexto anterior do caso"),
                List.of("peticao.pdf"),
                Map.of(
                        "ramo", "civel",
                        "rito", "comum",
                        "classe", "apelacao",
                        "sigilo", "publico",
                        "sourceSystem", "STJ",
                        "sourceUrl", "https://stj.jus.br"
                )
        ));

        assertEquals("conv-1", response.conversationId());
        assertEquals("V3", response.selectedVersion());
        assertEquals(5, response.virtualTrends().size());
        assertTrue(response.answer().contains("malha unificada do PJB"));
        assertTrue(response.safeguards().containsKey("hallucinationStatus"));
        assertEquals("CLEARED", response.safeguards().get("documentSecurityStatus"));
        assertNotNull(response.conversationContext().get("juridicaSpineProfile"));
        assertNotNull(response.conversationContext().get("conversationTrace"));
        assertNotNull(response.conversationContext().get("conversationMemory"));
        assertNotNull(response.conversationContext().get("conversationDocumentSecurity"));
        assertNotNull(response.conversationContext().get("conversationToolScope"));
        assertNotNull(response.conversationContext().get("conversationSessionDoctor"));
        assertNotNull(response.conversationContext().get("conversationSessionBootstrap"));
        assertNotNull(response.conversationContext().get("conversationCapabilityRecovery"));
        assertNotNull(response.conversationContext().get("conversationCapabilityCooldown"));
        assertNotNull(response.conversationContext().get("conversationCapabilityRehabilitation"));
        assertNotNull(response.conversationContext().get("conversationCapabilityRecurrence"));
        assertNotNull(response.conversationContext().get("conversationCapabilitySuppression"));
        assertNotNull(response.conversationContext().get("conversationTrustZone"));
        assertNotNull(response.conversationContext().get("conversationEvidenceProvenance"));
        assertNotNull(response.conversationContext().get("juridicaStructuredSchemaCatalog"));
        assertNotNull(response.conversationContext().get("juridicaRecommendedSchema"));
        assertNotNull(response.conversationContext().get("juridicaMcpPlan"));
        assertNotNull(response.conversationContext().get("conversationMcpPlan"));
        assertEquals("LEGAL_AI_DRAFT_ENVELOPE", response.safeguards().get("recommendedSchemaId"));
        assertEquals("DISCOVERY_THEN_PIN", response.safeguards().get("mcpSelectionMode"));
        assertNotNull(response.safeguards().get("mcpSkillIds"));
        assertNotNull(response.safeguards().get("mcpToolExampleIds"));
        assertNotNull(response.safeguards().get("mcpDeliberationMode"));
        assertNotNull(response.safeguards().get("mcpContextCompactionPolicy"));
        assertNotNull(response.safeguards().get("sessionDoctorStatus"));
        assertNotNull(response.safeguards().get("sessionDoctorOperationalMode"));
        assertNotNull(response.safeguards().get("sessionBootstrapStatus"));
        assertNotNull(response.safeguards().get("sessionBootstrapOperationalMode"));
        assertNotNull(response.safeguards().get("capabilityCooldownStatus"));
        assertNotNull(response.safeguards().get("capabilityRehabilitationStatus"));
        assertNotNull(response.safeguards().get("capabilityRehabilitationWindowTurnsRemaining"));
        assertNotNull(response.safeguards().get("capabilityRecurrenceStatus"));
        assertNotNull(response.safeguards().get("capabilityRecurrenceRiskTier"));
        assertNotNull(response.safeguards().get("capabilitySuppressionStatus"));
        assertNotNull(response.safeguards().get("capabilitySuppressionPolicyTier"));
        assertNotNull(response.safeguards().get("trustZoneStatus"));
        assertNotNull(response.safeguards().get("trustZone"));
        assertNotNull(response.safeguards().get("trustZoneMode"));
        assertNotNull(response.safeguards().get("evidenceProvenanceStatus"));
        assertNotNull(response.safeguards().get("evidenceProvenanceTier"));
        assertNotNull(response.safeguards().get("groundingPromotionStatus"));
        assertNotNull(response.safeguards().get("draftPromotionStatus"));
        assertFalse(response.nextSteps().isEmpty());
    }
}

