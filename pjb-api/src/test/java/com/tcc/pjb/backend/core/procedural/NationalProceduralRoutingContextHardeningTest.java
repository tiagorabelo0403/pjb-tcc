package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingContextHardeningTest {

    @Test
    void mustSanitizePayloadAndTrimResidualStringsOnForumContext() {
        NationalProceduralForumAllocationContext context = new NationalProceduralForumAllocationContext(
                Map.of("classe", "  indenizacao  ", "nested", Map.of("x", "  y  ")),
                "  corpus  ",
                new ProceduralCanonicalResolver.CanonicalContext(
                        Instant.now(),
                        RitoProcessual.COMUM_ORDINARIO,
                        "CIVEL",
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
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.8d, List.of(), List.of(), Map.of()),
                TipoJustica.ESTADUAL,
                " COMUM_ORDINARIO ",
                new NationalProceduralActionProfile("INDENIZATORIA", "CIVEL", false, "COMUM_ORDINARIO", "CIVEL", List.of(), List.of(), List.of(), List.of(), List.of()),
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.7d, false),
                " Fortaleza ",
                " CE ",
                " TJCE ",
                " TJCE ",
                "  ",
                " CIVEL ",
                null
        );

        assertEquals("indenizacao", context.payload().get("classe"));
        assertEquals(Map.of("x", "y"), context.payload().get("nested"));
        assertEquals("corpus", context.corpus());
        assertEquals("COMUM_ORDINARIO", context.ritoSugerido());
        assertEquals("Fortaleza", context.cidadeBase());
        assertNull(context.varaBase());
    }

    @Test
    void mustDeduplicateSignalsInReviewArtifacts() {
        NationalProceduralReviewDraft draft = new NationalProceduralReviewDraft(
                List.of(" razão ", "razão"),
                List.of(" base ", "base"),
                List.of(" alerta ", "alerta"),
                List.of(" checklist ", "checklist"),
                List.of(" bloqueio ", "bloqueio"),
                List.of(" marcador ", "marcador")
        );

        assertEquals(1, draft.reasons().size());
        assertEquals(1, draft.legalBases().size());
        assertEquals(1, draft.alerts().size());
        assertEquals(1, draft.reviewChecklist().size());
        assertEquals(1, draft.blockingIssues().size());
        assertEquals(1, draft.actionMarkers().size());
    }

    @Test
    void mustClampForumSeedDistributionScoreToNonNegative() {
        NationalProceduralForumAllocationSeed seed = new NationalProceduralForumAllocationSeed(
                null,
                new NationalProceduralTerritorialAnchor("BASE", "Fortaleza", "CE", "fundamento"),
                new NationalProceduralLinkageAnalysis("NENHUM", "NENHUM_SINAL", List.of(), List.of()),
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                null,
                null,
                null,
                -10.0d,
                null
        );

        assertFalse(seed.distributionScore() < 0.0d);
    }
}
