package com.tcc.pjb.backend.core.processo.juizado.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class NationalProceduralJuizadoTrackClassifierTest {

    @Test
    void mustClassifyCriminalTrackWhenActionNatureIsMinorOffense() {
        NationalProceduralJuizadoTrackClassifier classifier = new NationalProceduralJuizadoTrackClassifier();
        NationalProceduralJuizadoTrackLane lane = classifier.classify(new NationalProceduralJuizadoDecisionContext(
                Map.of("valorCausa", new BigDecimal("500.00")),
                new CompetenceResolveResponse("cmp-1", Instant.parse("2026-04-04T12:00:00Z"), TipoJustica.ESTADUAL.name(), "JUIZADO_ESPECIAL_CRIMINAL", 0.8d, List.of(), List.of(), Map.of()),
                new NationalProceduralActionProfile("INFRACAO_MENOR_POTENCIAL", "PENAL", false, "JUIZADO_ESPECIAL_CRIMINAL", "JECRIM", List.of(), List.of(), List.of(), List.of(), List.of()),
                new NationalProceduralPartyProfile(false, true, false, false, false, false, false, false, false, List.of(), null, null),
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("500.00"), LocalDate.of(2026, 4, 4)),
                "ameaça e injúria"
        ));

        assertEquals(NationalProceduralJuizadoTrackLane.CRIMINAL, lane);
    }
}
