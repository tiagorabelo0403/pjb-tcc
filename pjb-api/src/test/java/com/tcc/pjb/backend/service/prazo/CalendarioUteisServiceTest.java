package com.tcc.pjb.backend.service.prazo;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CalendarioUteisServiceTest {

    private final CalendarioUteisService service = new CalendarioUteisService();

    private CalendarioUteisService.CalendarioInput calSemFeriados() {
        return new CalendarioUteisService.CalendarioInput(
                "comarca-001", "CE", List.of(), List.of(), false, null, null);
    }

    @Test
    void isDiaUtil_retornaFalso_quandoSabado() {
        LocalDate sabado = LocalDate.of(2025, 1, 4);
        assertThat(service.isDiaUtil(sabado, calSemFeriados())).isFalse();
    }

    @Test
    void isDiaUtil_retornaFalso_quandoDomingo() {
        LocalDate domingo = LocalDate.of(2025, 1, 5);
        assertThat(service.isDiaUtil(domingo, calSemFeriados())).isFalse();
    }

    @Test
    void isDiaUtil_retornaVerdadeiro_quandoSegunda() {
        LocalDate segunda = LocalDate.of(2025, 1, 6);
        assertThat(service.isDiaUtil(segunda, calSemFeriados())).isTrue();
    }

    @Test
    void isDiaUtil_retornaFalso_quandoFeriadoNacional() {
        LocalDate feriado = LocalDate.of(2025, 1, 1);
        var cal = new CalendarioUteisService.CalendarioInput(
                "c", "CE", List.of(), List.of(feriado), false, null, null);
        assertThat(service.isDiaUtil(feriado, cal)).isFalse();
    }

    @Test
    void isDiaUtil_retornaFalso_quandoDentroDeFeriasForenses() {
        LocalDate diaFerias = LocalDate.of(2025, 1, 15);
        var cal = new CalendarioUteisService.CalendarioInput(
                "c", "CE", List.of(), List.of(), true,
                LocalDate.of(2025, 1, 2), LocalDate.of(2025, 1, 31));
        assertThat(service.isDiaUtil(diaFerias, cal)).isFalse();
    }

    @Test
    void proximoDiaUtil_avancaFinsDeSemana() {
        LocalDate sabado = LocalDate.of(2025, 1, 4);
        LocalDate resultado = service.proximoDiaUtil(sabado, calSemFeriados());
        assertThat(resultado.getDayOfWeek().getValue()).isLessThanOrEqualTo(5);
        assertThat(resultado).isEqualTo(LocalDate.of(2025, 1, 6));
    }

    @Test
    void calcularVencimento_conta15DiasUteis() {
        LocalDate inicio = LocalDate.of(2025, 2, 3);
        LocalDate vencimento = service.calcularVencimento(inicio, 15, calSemFeriados());
        assertThat(vencimento).isAfter(inicio);
    }

    @Test
    void diasUteisEntre_contaCorreto() {
        LocalDate seg = LocalDate.of(2025, 1, 6);
        LocalDate sex = LocalDate.of(2025, 1, 10);
        long count = service.diasUteisEntre(seg, sex, calSemFeriados());
        assertThat(count).isEqualTo(4);
    }
}
