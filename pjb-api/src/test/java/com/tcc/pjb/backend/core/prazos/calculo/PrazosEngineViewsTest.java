package com.tcc.pjb.backend.core.prazos.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoHealthQuery;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoWindowQuery;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazosEngineViewsTest {

    @Test
    void shouldExposeHealthWindowAndCalculationViews() {
        CalendarioForenseRepository repository = mock(CalendarioForenseRepository.class);
        when(repository.findApplicableBetween(any(), any(), any(), any()))
                .thenReturn(List.of(CalendarioForenseEntry.builder().dia(LocalDate.of(2026, 4, 21)).uf("SP").tipo("FERIADO").build()));
        PrazosEngine engine = new PrazosEngine(repository, "America/Sao_Paulo");

        var health = engine.health(new PrazoHealthQuery("SP", "Fortaleza"));
        var window = engine.window(new PrazoWindowQuery(LocalDateTime.of(2026, 4, 20, 10, 0), 2, PrazoRegime.UTEIS, "SP", "Fortaleza"));
        var calc = engine.calculationView(LocalDateTime.of(2026, 4, 20, 10, 0), 2, PrazoRegime.UTEIS, "SP", "Fortaleza");
        var calendar = engine.calendarioHealthView("SP", "Fortaleza");

        assertThat(health.calendarioDisponivel()).isTrue();
        assertThat(window.fim()).isAfter(window.inicio());
        assertThat(calc.regime()).isEqualTo(PrazoRegime.UTEIS);
        assertThat(calendar.entries()).isEqualTo(1L);
    }
}
