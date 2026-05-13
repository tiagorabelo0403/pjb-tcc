package com.tcc.pjb.backend.ai.juridica.mesh;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.juridica.eval.LegalBenchmarkCatalog;
import com.tcc.pjb.backend.ai.juridica.eval.LegalEvalReplayRunner;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpPlanScorer;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerDemotionPolicy;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerPromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.mcp.JuridicaMcpServerCatalogService;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JuridicaUnifiedMeshProfileServiceTest {

    private static JuridicaMcpServerCatalogService mcpCatalog() {
        return com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.mcpCatalog();
    }


    @Test
    void deveResolverPerfilProfundoParaV3ComFerramentasJuridicas() {
        JuridicaUnifiedMeshProfileService service = new JuridicaUnifiedMeshProfileService(new JuridicaLegalToolCatalogService(), mcpCatalog(), new LegalAiStructuredSchemaCatalog());
        IARequest request = IARequest.builder()
                .withOrigem("test")
                .withAcao("LEGAL_GENERAL_ASSIST_V3")
                .withPayload(Map.of("sigilo", true, "textoPeticaoLivre", "fatos estruturados"))
                .build();

        var profile = service.resolveForIa(
                request,
                ApiVersion.V3,
                "LEGAL_GENERAL_ASSIST_V3",
                Map.of("complexityScore", 88, "injectionRiskScore", 12),
                Map.of("curriculum", "civil"),
                Map.of("effectiveMode", "READ_ONLY")
        );

        assertEquals(ApiVersion.V3.name(), profile.version());
        assertFalse(profile.tools().isEmpty());
        assertTrue(profile.qualityFilters().contains("CITATION_FIRST_OUTPUT"));
        assertEquals("HYBRID_BM25_DENSE_RERANK_HIERARCHY", profile.rag().get("mode"));
        assertEquals("PINNED_STRICT_TRUST_CHAIN", ((java.util.Map<?, ?>) profile.mcp().get("plan")).get("selectionMode"));
        assertFalse(((java.util.List<?>) profile.mcp().get("servers")).isEmpty());
        assertNotNull(profile.mcp().get("doctorStatus"));
        assertEquals(Boolean.TRUE, profile.mcp().get("transcriptReplayReady"));
    }
}
