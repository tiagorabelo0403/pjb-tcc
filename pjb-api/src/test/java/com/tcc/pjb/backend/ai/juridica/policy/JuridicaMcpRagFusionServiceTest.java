package com.tcc.pjb.backend.ai.juridica.policy;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JuridicaMcpRagFusionServiceTest {

    @Test
    void deveOrquestrarRagEMcpEmModoSomenteLeituraQuandoCasoForComplexoEInstitucional() {
        JuridicaMcpRagFusionService service = com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.ragFusionService();

        JuridicaMcpRagFusionService.FusionPlan plan = service.resolve(
                new JuridicaMcpRagFusionService.ResolveRequest(
                        "PETICAO_ASSISTIDA",
                        ApiVersion.V3,
                        "PREVIDENCIARIO",
                        "COMUM",
                        "FEDERAL",
                        "BENEFICIO_PREVIDENCIARIO",
                        "PETICAO_PREVIDENCIARIA_BENEFICIO",
                        TipoUsuario.PROCURADORIA_FEDERAL,
                        88,
                        18,
                        false,
                        true,
                        Map.of(),
                        Map.of("effectiveMode", "READ_ONLY_GUARDED"),
                        Map.of(
                                "curriculum", Map.of(
                                        "ramoCodigo", "PREVIDENCIARIO",
                                        "materiasPrioritarias", List.of("benefício por incapacidade", "carência"),
                                        "legislacaoChave", List.of("Lei 8.213/91")
                                ),
                                "petitionBlueprint", Map.of(
                                        "procedureFamily", "BENEFICIO_PREVIDENCIARIO",
                                        "recommendedModelCode", "PETICAO_PREVIDENCIARIA_BENEFICIO",
                                        "requiredDocuments", List.of("laudo_medico", "cnis")
                                ),
                                "queryExpansionSeeds", List.of("PREVIDENCIARIO", "BENEFICIO_PREVIDENCIARIO")
                        )
                )
        );

        assertEquals("JURIDICA_STAGED_RAG_MCP_STRICT_V3", plan.profile());
        assertEquals("STAGED_RAG_THEN_MCP_READONLY", plan.executionMode());
        assertEquals("xhigh", plan.reasoningEffort());
        assertTrue(Boolean.TRUE.equals(plan.mcp().get("enabled")));
        assertTrue(Boolean.TRUE.equals(plan.mcp().get("toolSearchEnabled")));
        assertTrue(plan.queryExpansionSeeds().contains("PREVIDENCIARIO"));
        assertTrue(String.valueOf(plan.mcp().get("approvalMode")).contains("ALWAYS"));
    }

    @Test
    void deveFecharMcpQuandoModoNaoPermiteFerramentasRemotas() {
        JuridicaMcpRagFusionService service = com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.ragFusionService();

        JuridicaMcpRagFusionService.FusionPlan plan = service.resolve(
                new JuridicaMcpRagFusionService.ResolveRequest(
                        "CONSULTA_JURIDICA_V1",
                        ApiVersion.V1,
                        "CIVIL",
                        "COMUM",
                        "ESTADUAL",
                        "RESPONSABILIDADE_CIVIL",
                        "PETICAO_CIVEL_INDENIZATORIA",
                        TipoUsuario.ADVOGADO,
                        20,
                        5,
                        false,
                        false,
                        Map.of(),
                        Map.of("effectiveMode", "LOCAL_ONLY"),
                        Map.of()
                )
        );

        assertTrue(String.valueOf(plan.asMap().get("profile")).contains("LOCAL_ONLY") || String.valueOf(plan.asMap().get("profile")).contains("NO_MCP"));
        assertFalse(Boolean.TRUE.equals(plan.mcp().get("enabled")));
        assertEquals("RAG_ONLY_LOCAL", plan.executionMode());
    }
}
