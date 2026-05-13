package com.tcc.pjb.backend.ai.juridica.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiStructuredSchemaCatalogTest {

    private final LegalAiStructuredSchemaCatalog catalog = new LegalAiStructuredSchemaCatalog();

    @Test
    void mustExposeConcreteStructuredSchemasForV3() {
        var schemas = catalog.resolve(ApiVersion.V3);

        assertTrue(schemas.stream().anyMatch(item -> "LEGAL_AI_DESPACHO_SCHEMA".equals(item.schemaId())));
        assertTrue(schemas.stream().anyMatch(item -> "LEGAL_AI_DECISAO_SCHEMA".equals(item.schemaId())));
        assertTrue(schemas.stream().anyMatch(item -> "LEGAL_AI_DRAFT_ENVELOPE".equals(item.schemaId())));
        assertTrue(schemas.stream().anyMatch(item -> "LEGAL_AI_RISK_REPORT_SCHEMA".equals(item.schemaId())));
    }

    @Test
    void mustRecommendDecisionSchemaForMagistrateDecisionTurn() {
        var schema = catalog.recommend(
                ApiVersion.V3,
                "LEGAL_GENERAL_ASSIST_V3",
                new LegalAiConversationRequest(
                        "conv-112",
                        "proc-1",
                        "Preciso de uma decisão para julgar o pedido e enfrentar os pontos controvertidos.",
                        "MAGISTRADO",
                        List.of(),
                        List.of(),
                        Map.of()
                )
        );

        assertEquals("LEGAL_AI_DECISAO_SCHEMA", schema.schemaId());
        assertEquals("DECISAO", schema.stage());
    }
}
