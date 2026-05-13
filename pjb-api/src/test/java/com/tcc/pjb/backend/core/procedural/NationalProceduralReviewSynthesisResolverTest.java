package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralReviewSynthesisResolverTest {

    @Test
    void mustCentralizeBlockingAlertsChecklistAndConfidenceAssessment() {
        NationalProceduralReviewSynthesisResolver resolver = new NationalProceduralReviewSynthesisResolver(
                new NationalProceduralReviewSignalCollector(
                        new NationalProceduralReviewReasonCollector(),
                        new NationalProceduralReviewPolicySignalResolver(new NationalProceduralReviewMessages())
                ),
                new NationalProceduralReviewInputRequirementResolver(
                        new NationalProceduralReviewCoreFieldRequirementResolver(new NationalProceduralReviewMessages()),
                        new NationalProceduralReviewEconomicRequirementResolver(new NationalProceduralReviewMessages()),
                        new NationalProceduralReviewLocationRequirementResolver(),
                        new NationalProceduralReviewPartyRequirementResolver()
                ),
                new NationalProceduralConfidenceResolver()
        );

        NationalProceduralReviewSynthesis synthesis = resolver.resolve(
                NationalProceduralReviewSignalCollectorTest.baseContext(Map.of(
                        "tipoAcao", "fazenda",
                        "foro", "Fortaleza",
                        "ufAutor", "CE"
                ))
        );

        assertTrue(synthesis.blockingIssues().contains("Classe processual ausente para fechamento seguro da rota procedimental."));
        assertTrue(synthesis.blockingIssues().contains("Objeto processual insuficiente para consolidar competência material e vara sugerida."));
        assertTrue(synthesis.blockingIssues().contains("Pedido principal ausente para fechamento do rito, da alçada e da competência."));
        assertTrue(synthesis.blockingIssues().contains("Valor da causa ausente para validar aderência econômica do procedimento selecionado."));
        assertTrue(synthesis.alerts().contains("Distribuição dinâmica não retornou unidade cadastrada; manter sugestão de família de vara e revisar malha local."));
        assertTrue(synthesis.reviewChecklist().contains("Verificar se a especialização fazendária ou administrativa local exige vara exclusiva."));
        assertTrue(synthesis.reviewChecklist().contains("Conferir aderência do rito escolhido ao pedido e ao órgão jurisdicional."));
        assertTrue(synthesis.reviewChecklist().contains("Conferir distribuição por dependência, prevenção, conexão ou continência antes do protocolo final."));
        assertTrue(synthesis.actionMarkers().contains("MARCADOR"));
        assertEquals("ALTO", synthesis.riskLevel());
        assertTrue(synthesis.requiresHumanReview());
    }
}
