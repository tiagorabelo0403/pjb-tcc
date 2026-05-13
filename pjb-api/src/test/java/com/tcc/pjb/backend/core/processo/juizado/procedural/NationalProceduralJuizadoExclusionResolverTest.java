package com.tcc.pjb.backend.core.processo.juizado.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile;
import com.tcc.pjb.backend.core.procedural.NationalProceduralPartyProfile;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralJuizadoExclusionResolverTest {

    @Test
    void mustBlockJuizadoForSpecialExcludedNature() {
        NationalProceduralJuizadoExclusionResolver resolver = new NationalProceduralJuizadoExclusionResolver(
                new NationalProceduralJuizadoDecisionMessages()
        );

        NationalProceduralJuizadoDecision decision = resolver.resolve(new NationalProceduralJuizadoDecisionContext(
                Map.of(),
                new CompetenceResolveResponse("cmp-1", Instant.parse("2026-04-04T12:00:00Z"), TipoJustica.ESTADUAL.name(), "COMUM_ORDINARIO", 0.9d, List.of(), List.of(), Map.of()),
                new NationalProceduralActionProfile("MANDADO_SEGURANCA", "CONSTITUCIONAL", true, "ESPECIAL_MANDADO_SEGURANCA", "MANDADO_SEGURANCA", List.of(), List.of(), List.of(), List.of(), List.of()),
                new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, false, List.of(), null, null),
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("1000.00"), LocalDate.of(2026, 4, 4)),
                "mandado de seguranca"
        )).orElseThrow();

        assertTrue(decision.requiresReview());
        assertFalse(decision.admiteJuizado());
        assertTrue(decision.alerts().contains("A natureza da ação aponta trilha especial ou excluída do regime dos juizados."));
    }
}
