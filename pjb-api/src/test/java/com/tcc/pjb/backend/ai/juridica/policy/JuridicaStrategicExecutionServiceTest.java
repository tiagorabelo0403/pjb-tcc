package com.tcc.pjb.backend.ai.juridica.policy;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JuridicaStrategicExecutionServiceTest {

    @Test
    void deveEscalarParaProtocoloEstritoQuandoHaPeticaoAnexosERisco() {
        JuridicaStrategicExecutionService service = com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.strategicExecutionService();

        JuridicaStrategicExecutionService.StrategyPlan plan = service.resolve(
                new JuridicaStrategicExecutionService.ResolveRequest(
                        "PROTOCOLO_ASSISTIDO",
                        ApiVersion.V3,
                        "PREVIDENCIARIO",
                        "COMUM",
                        "FEDERAL",
                        "BENEFICIO_PREVIDENCIARIO",
                        "PETICAO_PREVIDENCIARIA_BENEFICIO",
                        TipoUsuario.PROCURADORIA_FEDERAL,
                        86,
                        42,
                        false,
                        true,
                        Map.of(
                                "textoPeticaoLivre", "x".repeat(12000),
                                "documentosAnexados", List.of("inicial.pdf", "cnis.pdf", "laudo.pdf", "procuração.pdf"),
                                "prepararPacoteProtocolo", true
                        ),
                        Map.of("profile", "RAG_STRICT_MULTISTAGE_V3"),
                        Map.of(
                                "retrieval", Map.of(
                                        "connectorFamilies", List.of("LEGAL_CURRICULUM", "TRF_AND_TNU")
                                )
                        ),
                        Map.of(
                                "curriculum", Map.of(
                                        "materiasPrioritarias", List.of("benefício por incapacidade"),
                                        "legislacaoChave", List.of("Lei 8.213/91")
                                ),
                                "petitionBlueprint", Map.of(
                                        "requiredDocuments", List.of("laudo_medico", "cnis")
                                )
                        )
                )
        );

        assertEquals("LEGAL_PROTOCOL_AND_DOC_HEAVY_STRICT_V4", plan.profile());
        assertEquals("PROTOCOL_STAGED_BATCH_INGESTION", plan.ingestion().get("mode"));
        assertEquals("PROTOCOL_VERIFIER_ABSOLUTE", plan.verifier().get("mode"));
        assertTrue(Boolean.TRUE.equals(plan.protocol().get("enabled")));
        assertTrue(plan.queryHints().contains("competencia_prevenção_redistribuição_protocolo"));
        assertTrue(plan.readingGoals().contains("verificacao_final_de_protocolo_e_distribuicao"));
    }

    @Test
    void deveFicarBalanceadoEmConsultaSimplesSemLoteNemProtocolo() {
        JuridicaStrategicExecutionService service = com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.strategicExecutionService();

        JuridicaStrategicExecutionService.StrategyPlan plan = service.resolve(
                new JuridicaStrategicExecutionService.ResolveRequest(
                        "CONSULTA_JURIDICA_V1",
                        ApiVersion.V1,
                        "FAMILIA",
                        "COMUM",
                        "ESTADUAL",
                        "ALIMENTOS",
                        "PETICAO_FAMILIA_ALIMENTOS",
                        TipoUsuario.ADVOGADO,
                        22,
                        4,
                        false,
                        false,
                        Map.of("pergunta", "Quais são os requisitos básicos para pedir alimentos?"),
                        Map.of(),
                        Map.of(),
                        Map.of()
                )
        );

        assertEquals("LEGAL_BALANCED_EXECUTION_V2", plan.profile());
        assertEquals("FOCUSED_SINGLE_DOCUMENT_INGESTION", plan.ingestion().get("mode"));
        assertEquals("LEGAL_VERIFIER_STRICT", plan.verifier().get("mode"));
        assertFalse(Boolean.TRUE.equals(plan.protocol().get("enabled")));
        assertTrue(String.valueOf(plan.cache().get("mode")).contains("QUERY"));
    }
}
