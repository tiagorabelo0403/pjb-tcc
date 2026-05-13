package com.tcc.pjb.backend.core.prazos.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PrazosEngineIT extends PjbIntegrationTestBase {

    @Autowired
    private PrazosEngine engine;

    @Test
    void calcularPrazoUteis_semFeriados_devePularFinaisDeSemana() {
        LocalDateTime segunda = LocalDateTime.of(2026, 4, 13, 8, 0);
        LocalDateTime resultado = engine.calcularTermino(segunda, 5, PrazoRegime.UTEIS, "SP", null);
        assertThat(resultado.getDayOfWeek()).isNotEqualTo(DayOfWeek.SATURDAY).isNotEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    void calcularPrazoEca_deveUsarDiasCorridos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 10, 8, 0);
        LocalDateTime uteis = engine.calcularTermino(inicio, 10, PrazoRegime.UTEIS, "SP", null);
        LocalDateTime eca = engine.calcularTermino(inicio, 10, PrazoRegime.ECA, "SP", null);
        assertThat(eca).isBeforeOrEqualTo(uteis);
    }
}
