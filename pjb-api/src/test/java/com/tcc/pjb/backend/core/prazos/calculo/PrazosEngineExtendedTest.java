package com.tcc.pjb.backend.core.prazos.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazosEngineExtendedTest {

    @Test
    void shouldDoubleUsefulDaysForDobroUteis() {
        CalendarioForenseRepository repository = mock(CalendarioForenseRepository.class);
        when(repository.findApplicableBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        PrazosEngine engine = new PrazosEngine(repository, "America/Sao_Paulo");
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 13, 8, 0);
        LocalDateTime fim = engine.calcularTermino(inicio, 5, PrazoRegime.DOBRO_UTEIS, "CE", null);
        assertThat(fim.toLocalDate()).isEqualTo(LocalDate.of(2026, 4, 27));
    }

    @Test
    void shouldAddHoursForHorasRegime() {
        CalendarioForenseRepository repository = mock(CalendarioForenseRepository.class);
        PrazosEngine engine = new PrazosEngine(repository, "America/Sao_Paulo");
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 13, 8, 0);
        assertThat(engine.calcularTermino(inicio, 6, PrazoRegime.HORAS, "CE", null)).isEqualTo(LocalDateTime.of(2026, 4, 13, 14, 0));
    }
}
