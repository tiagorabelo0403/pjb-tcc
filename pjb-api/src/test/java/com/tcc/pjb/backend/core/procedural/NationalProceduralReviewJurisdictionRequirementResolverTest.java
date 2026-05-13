package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralReviewJurisdictionRequirementResolverTest {

    @Test
    void mustDemandElectoralSignalsBeforeClosingJurisdiction() {
        NationalProceduralReviewJurisdictionRequirementResolver resolver = new NationalProceduralReviewJurisdictionRequirementResolver(new NationalProceduralReviewMessages());
        NationalProceduralReviewSynthesisContext context = new NationalProceduralReviewSynthesisContext(
                Map.of(
                        "classe", "AIJE",
                        "pedidoPrincipal", "cassação",
                        "parteAutoraNome", "Coligação",
                        "parteReuNome", "Candidato"
                ),
                NationalProceduralReviewSignalCollectorTest.baseContext(Map.of()).selectedRito(),
                NationalProceduralReviewSignalCollectorTest.baseContext(Map.of()).competence(),
                new NationalProceduralActionProfile(
                        "AIJE",
                        "ELEITORAL",
                        true,
                        "ELEITORAL_AIJE",
                        "ELEITORAL",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of()
                ),
                NationalProceduralReviewSignalCollectorTest.baseContext(Map.of()).juizadoDecision(),
                NationalProceduralReviewSignalCollectorTest.baseContext(Map.of()).partyProfile(),
                NationalProceduralReviewSignalCollectorTest.baseContext(Map.of()).teto(),
                NationalProceduralReviewSignalCollectorTest.baseContext(Map.of()).forumAllocation(),
                null,
                TipoJustica.ELEITORAL,
                "Fortaleza",
                "CE"
        );

        NationalProceduralReviewInputSlice slice = resolver.assess(context);

        assertTrue(slice.missingInputs().contains("zonaOuMunicipioEleitoral"));
        assertTrue(slice.missingInputs().contains("anoPleito"));
        assertTrue(slice.missingInputs().contains("cargoOuMandatoEleitoral"));
        assertTrue(slice.blockingIssues().contains("Justiça Eleitoral exige zona eleitoral ou município-base do pleito para fechar competência e distribuição."));
    }

    @Test
    void mustDemandMilitaryScopeBeforeRoutingMilitaryCase() {
        NationalProceduralReviewJurisdictionRequirementResolver resolver = new NationalProceduralReviewJurisdictionRequirementResolver(new NationalProceduralReviewMessages());
        NationalProceduralReviewSynthesisContext base = NationalProceduralReviewSignalCollectorTest.baseContext(Map.of());
        NationalProceduralReviewSynthesisContext context = new NationalProceduralReviewSynthesisContext(
                Map.of(
                        "classe", "IPM",
                        "pedidoPrincipal", "apuração",
                        "parteAutoraNome", "Autoridade Militar",
                        "parteReuNome", "Investigado"
                ),
                base.selectedRito(),
                base.competence(),
                new NationalProceduralActionProfile(
                        "IPM",
                        "MILITAR",
                        true,
                        "MILITAR_IPM",
                        "MILITAR",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of()
                ),
                base.juizadoDecision(),
                base.partyProfile(),
                base.teto(),
                base.forumAllocation(),
                null,
                TipoJustica.MILITAR_FEDERAL,
                "Fortaleza",
                "CE"
        );

        NationalProceduralReviewInputSlice slice = resolver.assess(context);

        assertTrue(slice.missingInputs().contains("escopoJusticaMilitar"));
        assertTrue(slice.missingInputs().contains("corporacaoOuForcaMilitar"));
        assertTrue(slice.blockingIssues().contains("É necessário indicar se o caso pertence à Justiça Militar da União ou à Justiça Militar Estadual."));
    }

    @Test
    void mustAcceptNoviceFactualSignalsForElectoralAndMilitary() {
        NationalProceduralReviewJurisdictionRequirementResolver resolver = new NationalProceduralReviewJurisdictionRequirementResolver(new NationalProceduralReviewMessages());
        NationalProceduralReviewSynthesisContext base = NationalProceduralReviewSignalCollectorTest.baseContext(Map.of());

        NationalProceduralReviewSynthesisContext eleitoral = new NationalProceduralReviewSynthesisContext(
                Map.of(
                        "descricaoProblemaEleitoral", "Propaganda irregular em rádio local",
                        "municipioCandidatura", "Morada Nova",
                        "anoEleicao", "2026",
                        "cargoEmDisputa", "Deputado estadual"
                ),
                base.selectedRito(),
                base.competence(),
                new NationalProceduralActionProfile(
                        "AIJE",
                        "ELEITORAL",
                        true,
                        "ELEITORAL_AIJE",
                        "ELEITORAL",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of()
                ),
                base.juizadoDecision(),
                base.partyProfile(),
                base.teto(),
                base.forumAllocation(),
                null,
                TipoJustica.ELEITORAL,
                "Fortaleza",
                "CE"
        );

        NationalProceduralReviewInputSlice eleitoralSlice = resolver.assess(eleitoral);
        assertTrue(eleitoralSlice.missingInputs().isEmpty());

        NationalProceduralReviewSynthesisContext militar = new NationalProceduralReviewSynthesisContext(
                Map.of(
                        "casoMilitarDaUniaoOuDoEstado", "ESTADUAL",
                        "policiaMilitarEstado", "Polícia Militar do Ceará",
                        "agenteEraMilitarOuCivil", "MILITAR",
                        "cidadeOcorrencia", "Fortaleza",
                        "tipoProblemaMilitar", "disciplinar"
                ),
                base.selectedRito(),
                base.competence(),
                new NationalProceduralActionProfile(
                        "IPM",
                        "MILITAR",
                        true,
                        "MILITAR_IPM",
                        "MILITAR",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of()
                ),
                base.juizadoDecision(),
                base.partyProfile(),
                base.teto(),
                base.forumAllocation(),
                null,
                TipoJustica.MILITAR_ESTADUAL,
                "Fortaleza",
                "CE"
        );

        NationalProceduralReviewInputSlice militarSlice = resolver.assess(militar);
        assertTrue(militarSlice.missingInputs().isEmpty());
    }

}
