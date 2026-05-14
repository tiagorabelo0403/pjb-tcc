package com.tcc.pjb.backend.service.prazo;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrazoIntempestividadeGuardServiceTest {

    private final CalendarioUteisService calendario = new CalendarioUteisService();
    private final PrazoIntempestividadeGuardService service =
            new PrazoIntempestividadeGuardService(calendario);

    @Test
    void avaliar_tempestivo_quandoAtoNoDia() {
        var input = new PrazoIntempestividadeGuardService.IntempestividadeInput(
                LocalDate.of(2025, 3, 10), LocalDate.of(2025, 3, 10),
                false, false, null);
        var result = service.avaliar(input);
        assertThat(result.tempestivo()).isTrue();
        assertThat(result.diasAtraso()).isEqualTo(0);
    }

    @Test
    void avaliar_intempestivo_quandoAtoPosVencimento() {
        var input = new PrazoIntempestividadeGuardService.IntempestividadeInput(
                LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 10),
                false, false, null);
        var result = service.avaliar(input);
        assertThat(result.tempestivo()).isFalse();
        assertThat(result.diasAtraso()).isEqualTo(5);
    }

    @Test
    void avaliar_fundamentacaoContémFazendaPublica_quandoIntempestivo() {
        var input = new PrazoIntempestividadeGuardService.IntempestividadeInput(
                LocalDate.of(2025, 4, 20), LocalDate.of(2025, 4, 10),
                true, false, null);
        var result = service.avaliar(input);
        assertThat(result.fundamentacao()).anyMatch(f -> f.contains("Fazenda"));
    }

    @Test
    void avaliar_admiteJustificativa_quandoJustificativaFornecida() {
        var input = new PrazoIntempestividadeGuardService.IntempestividadeInput(
                LocalDate.of(2025, 4, 20), LocalDate.of(2025, 4, 10),
                false, false, "Greve nacional de advogados");
        var result = service.avaliar(input);
        assertThat(result.admiteJustificativa()).isTrue();
    }

    @Test
    void avaliar_semFundamentacao_quandoTempestivo() {
        var input = new PrazoIntempestividadeGuardService.IntempestividadeInput(
                LocalDate.of(2025, 3, 5), LocalDate.of(2025, 3, 10),
                false, false, null);
        var result = service.avaliar(input);
        assertThat(result.fundamentacao()).isEmpty();
    }
}
