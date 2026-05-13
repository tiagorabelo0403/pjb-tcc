package com.tcc.pjb.backend.core.prazos.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoHealthQuery;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoRegimeView;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazosEnginePolicyAndHealthTest {

    @Test
    void shouldExposeHealthPolicyAndWindowViews() {
        CalendarioForenseRepository calendario = mock(CalendarioForenseRepository.class);
        when(calendario.findApplicableBetween("CE", "Fortaleza", LocalDate.now().minusDays(30), LocalDate.now().plusDays(30)))
                .thenReturn(List.of(new CalendarioForenseEntry(LocalDate.now().plusDays(1), "CE", "Fortaleza", "FERIADO")));
        when(calendario.findApplicableBetween("CE", "Fortaleza", LocalDate.now().minusDays(365), LocalDate.now().plusDays(365)))
                .thenReturn(List.of(new CalendarioForenseEntry(LocalDate.now().plusDays(2), "CE", "Fortaleza", "FERIADO")));
        when(calendario.findApplicableBetween("CE", "Fortaleza", LocalDate.of(2026, 4, 9), LocalDate.of(2026, 5, 20)))
                .thenReturn(List.of(new CalendarioForenseEntry(LocalDate.of(2026, 4, 14), "CE", "Fortaleza", "FERIADO")));
        PrazosEngine engine = new PrazosEngine(calendario, "America/Fortaleza");

        var health = engine.health(new PrazoHealthQuery("CE", "Fortaleza"));
        var policy = engine.policyView("CIVIL", "COMUM", PrazoRegime.UTEIS);
        var window = engine.window(new com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoWindowQuery(LocalDateTime.of(2026, 4, 11, 9, 0), 2, PrazoRegime.UTEIS, "CE", "Fortaleza"));
        PrazoRegimeView regime = engine.regimeView(PrazoRegime.DOBRO_UTEIS);

        assertThat(health.healthy()).isTrue();
        assertThat(policy.ramo()).isEqualTo("CIVIL");
        assertThat(window.fim()).isAfter(window.inicio());
        assertThat(regime.label()).isEqualTo("DOBRO_UTEIS");
    }
}
