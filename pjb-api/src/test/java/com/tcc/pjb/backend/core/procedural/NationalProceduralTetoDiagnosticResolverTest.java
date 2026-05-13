package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralTetoDiagnosticResolverTest {

    @Test
    void mustDelegateDiagnosticWithResolvedCanonicalInputs() {
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        TetoProcessualService.DiagnosticoTetoProcessual diagnostico = mock(TetoProcessualService.DiagnosticoTetoProcessual.class);
        when(tetoProcessualService.diagnosticar(any(BigDecimal.class), any(com.tcc.pjb.backend.domain.enums.TipoJustica.class), any(RamoDireito.class), anyString(), any(), any(LocalDate.class)))
                .thenReturn(diagnostico);
        NationalProceduralTetoDiagnosticResolver resolver = new NationalProceduralTetoDiagnosticResolver(tetoProcessualService);
        LocalDate referencia = LocalDate.of(2026, 4, 4);

        TetoProcessualService.DiagnosticoTetoProcessual resultado = resolver.resolve(
                new NationalProceduralTetoDiagnosticContext(
                        Map.of(
                                "valorCausa", new BigDecimal("42000.00"),
                                "tipoJustica", "TRABALHO",
                                "ramoDireito", "TRABALHISTA",
                                "rito", "RECLAMACAO_TRABALHISTA",
                                "__dataReferencia", referencia
                        ),
                        competence(),
                        canonical(),
                        selectedRito()
                )
        );

        assertSame(diagnostico, resultado);
        verify(tetoProcessualService).diagnosticar(
                eq(new BigDecimal("42000.00")),
                eq(com.tcc.pjb.backend.domain.enums.TipoJustica.TRABALHO),
                eq(RamoDireito.TRABALHISTA),
                eq("RECLAMACAO_TRABALHISTA"),
                isNull(),
                eq(referencia)
        );
    }

    private static CompetenceResolveResponse competence() {
        return new CompetenceResolveResponse("req", Instant.now(), "TRABALHO", "RECLAMACAO_TRABALHISTA", 0.9d, List.of(), List.of(), Map.of());
    }

    private static ProceduralCanonicalResolver.CanonicalContext canonical() {
        return new ProceduralCanonicalResolver.CanonicalContext(
                Instant.now(),
                RitoProcessual.RECLAMACAO_TRABALHISTA,
                "TRABALHISTA",
                "123",
                "Reclamacao Trabalhista",
                "TRABALHO",
                "TRT7",
                "TRT7",
                "PJE",
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
    }

    private static CanonicalRitoSelector.SelectedRito selectedRito() {
        return new CanonicalRitoSelector.SelectedRito(
                Instant.now(),
                "test",
                canonical(),
                new CanonicalSanityGate.GateResult("OK", true, List.of(), Map.of(), Instant.now()),
                RitoProcessual.RECLAMACAO_TRABALHISTA,
                "CANONICAL_RITO_RESOLVED",
                false,
                false,
                Map.of()
        );
    }
}
