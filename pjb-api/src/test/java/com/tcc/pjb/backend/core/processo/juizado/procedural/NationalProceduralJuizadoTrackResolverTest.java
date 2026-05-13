package com.tcc.pjb.backend.core.processo.juizado.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile;
import com.tcc.pjb.backend.core.procedural.NationalProceduralPartyProfile;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralJuizadoTrackResolverTest {

    @Test
    void mustCloseFederalJuizadoWithComplexEvidenceAsReviewRequired() {
        NationalProceduralJuizadoDecisionMessages messages = new NationalProceduralJuizadoDecisionMessages();
        NationalProceduralJuizadoTrackResolver resolver = new NationalProceduralJuizadoTrackResolver(
                new NationalProceduralJuizadoTrackClassifier(),
                new NationalProceduralJuizadoFederalTrackResolver(messages),
                new NationalProceduralJuizadoFazendaTrackResolver(messages),
                new NationalProceduralJuizadoCivelTrackResolver(messages),
                new NationalProceduralJuizadoCriminalTrackResolver(messages)
        );
        TetoProcessualService.DiagnosticoTetoProcessual teto = new TetoProcessualService.DiagnosticoTetoProcessual(
                "TETO-JEF",
                false,
                false,
                false,
                null,
                2026,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TipoJustica.FEDERAL.name(),
                "JUIZADO_ESPECIAL_FEDERAL",
                "Lei 10.259/2001",
                "seguir",
                "hash",
                Instant.parse("2026-04-04T12:00:00Z")
        );

        NationalProceduralJuizadoDecision decision = resolver.resolve(new NationalProceduralJuizadoDecisionContext(
                Map.of("valorCausa", new BigDecimal("10000.00")),
                new CompetenceResolveResponse("cmp-1", Instant.parse("2026-04-04T12:00:00Z"), TipoJustica.FEDERAL.name(), "JUIZADO_ESPECIAL_FEDERAL", 0.91d, List.of(), List.of(), Map.of()),
                new NationalProceduralActionProfile("PREVIDENCIARIO", "PREVIDENCIARIO", false, "PREVIDENCIARIO_JEF", "PREVIDENCIARIO", List.of(), List.of(), List.of(), List.of(), List.of()),
                new NationalProceduralPartyProfile(true, false, false, false, false, false, false, false, false, List.of(), null, null),
                teto,
                "pedido federal com pericia complexa e engenharia complexa"
        ));

        assertEquals("JUIZADO_ESPECIAL_FEDERAL", decision.ritoOverride());
        assertTrue(decision.requiresReview());
        assertTrue(decision.alerts().contains("Prova técnica densa recomenda revisão humana antes do fechamento em JEF."));
    }
}
