package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralConfidenceResolverTest {

    @Test
    void mustKeepLowRiskWhenSignalsAreConsistent() {
        NationalProceduralConfidenceResolver resolver = new NationalProceduralConfidenceResolver();

        NationalProceduralConfidenceAssessment assessment = resolver.assess(
                selectedRito(false, false),
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.91d, List.of(), List.of(), Map.of()),
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.89d, false),
                forumAllocation(true, true, true, List.of(), List.of()),
                new NationalProceduralDistributionSuggestion("V1", "TJCE", "FORTALEZA", "CE", "CIVEL", 0.88d, "ok", List.of(), List.of()),
                List.of(),
                List.of(),
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("1000.00"), LocalDate.now())
        );

        assertTrue(assessment.confidence() > 0.70d);
        assertEquals("BAIXO", assessment.riskLevel());
        assertFalse(assessment.requiresHumanReview());
    }

    @Test
    void mustEscalateToCriticalWhenForumAllocationHasIncompatibilities() {
        NationalProceduralConfidenceResolver resolver = new NationalProceduralConfidenceResolver();

        NationalProceduralConfidenceAssessment assessment = resolver.assess(
                selectedRito(true, true),
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.58d, List.of(), List.of(), Map.of()),
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.52d, true),
                forumAllocation(false, false, false, List.of("incompatibilidade estrutural"), List.of("123")),
                null,
                List.of("classeProcessual", "valorCausa"),
                List.of("alerta"),
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("0.00"), LocalDate.now())
        );

        assertTrue(assessment.confidence() < 0.65d);
        assertEquals("CRITICO", assessment.riskLevel());
        assertTrue(assessment.requiresHumanReview());
    }

    private static CanonicalRitoSelector.SelectedRito selectedRito(boolean heuristicUsed, boolean fallbackApplied) {
        return new CanonicalRitoSelector.SelectedRito(
                Instant.now(),
                "test",
                new ProceduralCanonicalResolver.CanonicalContext(
                        Instant.now(),
                        RitoProcessual.COMUM_ORDINARIO,
                        "CIVIL",
                        "7",
                        "Procedimento Comum",
                        "ESTADUAL",
                        "TJCE",
                        "TJCE",
                        "PJE",
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                null,
                RitoProcessual.COMUM_ORDINARIO,
                "CANONICAL_RITO_RESOLVED",
                heuristicUsed,
                fallbackApplied,
                Map.of()
        );
    }

    private static ProceduralForumAllocationReport forumAllocation(boolean preProtocoloApto,
                                                                   boolean distribuicaoAutomatica,
                                                                   boolean connectorOperational,
                                                                   List<String> incompatibilities,
                                                                   List<String> relatedProcessNumbers) {
        return new ProceduralForumAllocationReport(
                Instant.now(),
                "7",
                "Procedimento Comum",
                "TERRITORIAL_OK",
                "Fortaleza",
                "CE",
                "fundamento",
                "NENHUM",
                relatedProcessNumbers.isEmpty() ? "NENHUM_SINAL" : "CONEXAO",
                relatedProcessNumbers,
                "TJCE",
                "TJCE",
                "VARA-1",
                "1ª Vara Cível",
                "CIVEL",
                false,
                distribuicaoAutomatica,
                0.91d,
                "PJE",
                connectorOperational,
                false,
                false,
                true,
                preProtocoloApto,
                preProtocoloApto ? "READY_WITH_REVIEW" : "STRUCTURAL_REVIEW_REQUIRED",
                incompatibilities,
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
