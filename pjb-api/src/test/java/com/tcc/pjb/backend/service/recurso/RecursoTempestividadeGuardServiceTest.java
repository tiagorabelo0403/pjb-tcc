package com.tcc.pjb.backend.service.recurso;

import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecursoTempestividadeGuardServiceTest {

    private final CalendarioUteisService calendarioService = new CalendarioUteisService();
    private final RecursoTempestividadeGuardService service =
            new RecursoTempestividadeGuardService(calendarioService);

    private final CalendarioUteisService.CalendarioInput cal =
            new CalendarioUteisService.CalendarioInput(
                    "VARA-001", "SP", List.of(), List.of(), false, null, null);

    @Test
    void avaliar_tempestivo_quandoDentroDoProazo() {
        var input = new RecursoTempestividadeGuardService.TempestividadeInput(
                UUID.randomUUID(), RecursoProcessualTipo.APELACAO,
                LocalDate.now(),
                false, false, false);
        var result = service.avaliar(input, cal);
        assertThat(result.tempestivo()).isTrue();
        assertThat(result.diasUteisPrazo()).isEqualTo(15);
    }

    @Test
    void avaliar_prazoQuadruplo_fazendaPublica() {
        var input = new RecursoTempestividadeGuardService.TempestividadeInput(
                UUID.randomUUID(), RecursoProcessualTipo.APELACAO,
                LocalDate.now(),
                true, false, false);
        var result = service.avaliar(input, cal);
        assertThat(result.diasUteisPrazo()).isEqualTo(60);
    }

    @Test
    void avaliar_prazoQuadruplo_ministerioPublico() {
        var input = new RecursoTempestividadeGuardService.TempestividadeInput(
                UUID.randomUUID(), RecursoProcessualTipo.APELACAO,
                LocalDate.now(),
                false, true, false);
        var result = service.avaliar(input, cal);
        assertThat(result.diasUteisPrazo()).isEqualTo(60);
    }

    @Test
    void avaliar_prazoDuplo_defensoria() {
        var input = new RecursoTempestividadeGuardService.TempestividadeInput(
                UUID.randomUUID(), RecursoProcessualTipo.APELACAO,
                LocalDate.now(),
                false, false, true);
        var result = service.avaliar(input, cal);
        assertThat(result.diasUteisPrazo()).isEqualTo(30);
    }

    @Test
    void avaliar_embargosDeclaracao_prazo5() {
        var input = new RecursoTempestividadeGuardService.TempestividadeInput(
                UUID.randomUUID(), RecursoProcessualTipo.EMBARGOS_DECLARACAO,
                LocalDate.now(),
                false, false, false);
        var result = service.avaliar(input, cal);
        assertThat(result.diasUteisPrazo()).isEqualTo(5);
    }

    @Test
    void avaliar_intempestivo_quandoPrazoPassado() {
        var input = new RecursoTempestividadeGuardService.TempestividadeInput(
                UUID.randomUUID(), RecursoProcessualTipo.APELACAO,
                LocalDate.now().minusDays(30),
                false, false, false);
        var result = service.avaliar(input, cal);
        assertThat(result.tempestivo()).isFalse();
        assertThat(result.diasUteisRestantes()).isZero();
    }
}
