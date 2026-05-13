package com.tcc.pjb.backend.ai.juridica.spine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaLegalToolCatalogService;
import com.tcc.pjb.backend.ai.juridica.eval.LegalBenchmarkCatalog;
import com.tcc.pjb.backend.ai.juridica.eval.LegalEvalReplayRunner;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpPlanScorer;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerDemotionPolicy;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerPromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.mcp.JuridicaMcpServerCatalogService;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.ai.juridica.symbolic.JuridicaSymbolicValidationExecutionService;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalCabimentoRuleEngine;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalCompetenciaRuleEngine;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalPrazoRuleEngine;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalProceduralCompatibilityEngine;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalSigiloRuleEngine;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JuridicaResearchValidationPipelineTest {

    private static JuridicaMcpServerCatalogService mcpCatalog() {
        return com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.mcpCatalog();
    }


    @Test
    void researchDossierMustExposeStructuredSchemasAndFindings() {
        var service = new JuridicaResearchDossierService(
                new StubVectorSearchService(),
                new JuridicaUnifiedMeshProfileService(new JuridicaLegalToolCatalogService(), mcpCatalog(), new LegalAiStructuredSchemaCatalog()),
                spineService(),
                new LegalAiStructuredSchemaCatalog()
        );

        var response = service.build(new LegalResearchDossierRequest(
                "cumprimento de sentença",
                "civel",
                "execucao de titulo judicial",
                "civel",
                "comum",
                Map.of(),
                3
        ));

        assertFalse(response.findings().isEmpty());
        assertTrue(response.recommendedSchemas().contains("LEGAL_AI_PROCEDURAL_PLAN"));
        assertTrue(response.trace().containsKey("recommendedStructuredSchema"));
        assertTrue(response.trace().containsKey("structuredSchemaCatalog"));
    }

    @Test
    void validationMustBlockWhenContradictoryAndKeepCitationFirst() {
        var service = new JuridicaValidationEnvelopeService(spineService(), symbolicExecutionService());

        var response = service.validate(new LegalValidationRequest(
                "Agravo de instrumento no juizado sem artigo ou precedente.",
                "civel",
                "juizado especial",
                "recurso inominado",
                "validar cabimento",
                "restrito",
                Map.of()
        ));

        assertTrue(response.citationFirst());
        assertTrue(response.status().equals("BLOCKED") || response.status().equals("NEEDS_COMPLEMENT"));
        assertFalse(response.symbolicEngines().isEmpty());
    }

    private JuridicaLegalAiSpineService spineService() {
        return new JuridicaLegalAiSpineService(
                new JuridicaPolicyVariableService(),
                new JuridicaToolRoutingService(new JuridicaLegalToolCatalogService()),
                new JuridicaStructuredOutputProfileService(new LegalAiStructuredSchemaCatalog()),
                new JuridicaHybridRetrievalProfileService(),
                new JuridicaMemoryIsolationProfileService(),
                new JuridicaSymbolicValidationProfileService(),
                new JuridicaGraphProfileService(),
                new JuridicaMultimodalProfileService(),
                new JuridicaEvaluationProfileService(),
                new JuridicaAntiHallucinationProfileService(),
                new JuridicaTraceApprovalService()
        );
    }

    private static final class StubVectorSearchService implements VectorSearchService {
        @Override
        public VectorSearchResult searchSimilarResult(String query, Map<String, Object> filtros, int topK) {
            return searchSimilarV2(query, filtros, topK);
        }

        @Override
        public VectorSearchResult searchSimilarV1(String query, Map<String, Object> filtros, int topK) {
            return searchSimilarV2(query, filtros, topK);
        }

        @Override
        public VectorSearchResult searchSimilarV2(String query, Map<String, Object> filtros, int topK) {
            return new VectorSearchResult(
                    query,
                    Instant.now(),
                    List.of(new ResultItem("DOC-1", "Cumprimento de sentença", "civel", 0.98, 0.91, 0.07)),
                    Map.of(),
                    Map.of(),
                    "V2"
            );
        }

        @Override
        public VectorSearchResult searchSimilarV3(String query, Map<String, Object> filtros, int topK) {
            return searchSimilarV2(query, filtros, topK);
        }
    }

    private JuridicaSymbolicValidationExecutionService symbolicExecutionService() {
        return new JuridicaSymbolicValidationExecutionService(List.of(
                new LegalPrazoRuleEngine(),
                new LegalCompetenciaRuleEngine(),
                new LegalCabimentoRuleEngine(),
                new LegalSigiloRuleEngine(),
                new LegalProceduralCompatibilityEngine()
        ));
    }
}
