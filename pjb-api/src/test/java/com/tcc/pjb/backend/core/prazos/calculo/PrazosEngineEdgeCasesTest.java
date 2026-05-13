package com.tcc.pjb.backend.core.prazos.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazosEngineEdgeCasesTest {

    @Test
    void shouldUseCorridosForEcaAndReturnSameWhenQuantidadeZero() {
        CalendarioForenseRepository repository = mock(CalendarioForenseRepository.class);
        when(repository.findApplicableBetween(any(), any(), any(), any())).thenReturn(List.of());
        PrazosEngine engine = new PrazosEngine(repository, "America/Sao_Paulo");
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 10, 9, 0);
        assertThat(engine.calcularTermino(inicio, 0, PrazoRegime.UTEIS, "CE", null)).isEqualTo(inicio);
        assertThat(engine.calcularTermino(inicio, 10, PrazoRegime.ECA, "CE", null)).isEqualTo(inicio.plusDays(10));
    }

    @Test
    void shouldTreatCltHorasUteisAsUsefulDaysCalculation() {
        CalendarioForenseRepository repository = mock(CalendarioForenseRepository.class);
        when(repository.findApplicableBetween(any(), any(), any(), any())).thenReturn(List.of());
        PrazosEngine engine = new PrazosEngine(repository, "America/Sao_Paulo");
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 13, 8, 0);
        assertThat(engine.calcularTermino(inicio, 8, PrazoRegime.CLT_HORAS_UTEIS, "CE", null))
                .isEqualTo(engine.calcularTermino(inicio, 8, PrazoRegime.UTEIS, "CE", null));
    }
}
