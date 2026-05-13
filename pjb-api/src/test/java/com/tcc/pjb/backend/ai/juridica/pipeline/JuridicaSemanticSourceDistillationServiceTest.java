package com.tcc.pjb.backend.ai.juridica.pipeline;

import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JuridicaSemanticSourceDistillationServiceTest {

    @Test
    void devePriorizarFontesMaisConfiaveisEAderentesAoRamo() {
        JuridicaSemanticSourceDistillationService service = new JuridicaSemanticSourceDistillationService();
        IARequest request = IARequest.builder()
                .withOrigem("TEST")
                .withAcao("PETICAO_ASSISTIDA")
                .withPayload(Map.of(
                        "ramoDireito", "FAMILIA",
                        "resolvedProcedureFamily", "ALIMENTOS",
                        "meshGovernance", Map.of(
                                "rag", Map.of(
                                        "distillationProfile", "SEMANTIC_SOURCE_DISTILLATION_STRICT_V3"
                                )
                        )
                ))
                .build();
        AgentExecutionContext ctx = new AgentExecutionContext(request, ApiVersion.V3, "PETICAO_ASSISTIDA", Instant.parse("2026-04-02T10:00:00Z"), Clock.fixed(Instant.parse("2026-04-02T10:00:00Z"), ZoneOffset.UTC));

        VectorSearchService.VectorSearchResult raw = new VectorSearchService.VectorSearchResult(
                "ação de alimentos",
                Instant.parse("2026-04-02T10:00:00Z"),
                List.of(
                        new VectorSearchService.ResultItem("1", "STJ alimentos avoengos e binômio necessidade possibilidade", "FAMILIA", 0.91, 0.88, 0.80),
                        new VectorSearchService.ResultItem("2", "Artigo genérico sobre processo civil", "CIVIL", 0.90, 0.62, 0.20),
                        new VectorSearchService.ResultItem("3", "TJCE guarda e alimentos provisórios", "FAMILIA", 0.86, 0.79, 0.50)
                ),
                Map.of(),
                Map.of(),
                "mock-v3"
        );

        var result = service.distill(ctx, "ação de alimentos", raw, 2);

        assertEquals(2, result.evidences().size());
        assertTrue(result.evidences().getFirst().getTitulo().contains("STJ"));
        assertFalse(result.metadata().isEmpty());
        assertTrue(result.metadata().containsKey("audits"));
    }
}
