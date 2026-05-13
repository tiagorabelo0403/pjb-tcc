package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralReviewInputRequirementResolverTest {

    @Test
    void mustDetectMissingMandatoryInputsAndGenerateBlockingIssues() {
        NationalProceduralReviewInputRequirementResolver resolver = new NationalProceduralReviewInputRequirementResolver(
                new NationalProceduralReviewCoreFieldRequirementResolver(new NationalProceduralReviewMessages()),
                new NationalProceduralReviewEconomicRequirementResolver(new NationalProceduralReviewMessages()),
                new NationalProceduralReviewLocationRequirementResolver(),
                new NationalProceduralReviewPartyRequirementResolver(),
                new NationalProceduralReviewJurisdictionRequirementResolver(new NationalProceduralReviewMessages())
        );

        NationalProceduralReviewInputAssessment assessment = resolver.assess(
                NationalProceduralReviewSignalCollectorTest.baseContext(Map.of(
                        "tipoAcao", "fazenda",
                        "foro", "Fortaleza",
                        "ufAutor", "CE"
                ))
        );

        assertTrue(assessment.missingInputs().contains("classeProcessual"));
        assertTrue(assessment.missingInputs().contains("assuntoOuObjetoProcessual"));
        assertTrue(assessment.missingInputs().contains("pedidoPrincipal"));
        assertTrue(assessment.missingInputs().contains("valorCausa"));
        assertTrue(assessment.blockingIssues().contains("Classe processual ausente para fechamento seguro da rota procedimental."));
        assertTrue(assessment.blockingIssues().contains("Objeto processual insuficiente para consolidar competência material e vara sugerida."));
        assertTrue(assessment.blockingIssues().contains("Pedido principal ausente para fechamento do rito, da alçada e da competência."));
        assertTrue(assessment.blockingIssues().contains("Valor da causa ausente para validar aderência econômica do procedimento selecionado."));
    }
}
