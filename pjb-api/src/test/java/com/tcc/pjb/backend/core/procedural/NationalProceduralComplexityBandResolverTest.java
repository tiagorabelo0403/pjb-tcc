package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralComplexityBandResolverTest {

    @Test
    void mustEscalateCriticalBandWhenSignalsAccumulate() {
        NationalProceduralComplexityBandResolver resolver = new NationalProceduralComplexityBandResolver();

        String band = resolver.resolve(new NationalProceduralComplexityContext(
                actionProfile(true),
                "MEDICA_COMPLEXA",
                new NationalProceduralPartyProfile(false, false, false, true, false, false, false, true, true, List.of("PARTE_ESTADUAL", "MATERIA_MILITAR"), "AUTOR", "REU"),
                Map.of("envolveSaude", true, "casoUrgente", true),
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("1000.00"), LocalDate.now()),
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.62d, true)
        ));

        assertEquals("CRITICA", band);
    }

    @Test
    void mustKeepLowBandWhenCaseIsSimple() {
        NationalProceduralComplexityBandResolver resolver = new NationalProceduralComplexityBandResolver();

        String band = resolver.resolve(new NationalProceduralComplexityContext(
                actionProfile(false),
                "DOCUMENTAL_SIMPLES",
                new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, false, List.of(), "AUTOR", "REU"),
                Map.of(),
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("500.00"), LocalDate.now()),
                new NationalProceduralJuizadoDecision(true, null, List.of(), List.of(), List.of(), List.of(), 0.91d, false)
        ));

        assertEquals("BAIXA", band);
    }

    private static NationalProceduralActionProfile actionProfile(boolean specialProcedure) {
        return new NationalProceduralActionProfile(
                "INDENIZATORIA",
                "CIVEL",
                specialProcedure,
                "COMUM_ORDINARIO",
                "CIVEL",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
