package com.tcc.pjb.backend.service.recurso;

import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecursoProcessualReadinessServiceTest {

    private final CalendarioUteisService calendarioService = new CalendarioUteisService();
    private final RecursoAdmissibilidadeService admissibilidadeService =
            new RecursoAdmissibilidadeService();
    private final RecursoTempestividadeGuardService tempestividadeService =
            new RecursoTempestividadeGuardService(calendarioService);
    private final RecursoDesertaoRadarService desertaoService =
            new RecursoDesertaoRadarService();
    private final RecursoProcessualReadinessService service =
            new RecursoProcessualReadinessService(admissibilidadeService,
                    tempestividadeService, desertaoService);

    private final CalendarioUteisService.CalendarioInput cal =
            new CalendarioUteisService.CalendarioInput(
                    "VARA-001", "SP", List.of(), List.of(), false, null, null);

    @Test
    void avaliar_pronto_quandoTudoOk() {
        var input = buildInput(true, LocalDate.now(), true, false, false);
        var result = service.avaliar(input);
        assertThat(result.pronto()).isTrue();
        assertThat(result.admissivel()).isTrue();
        assertThat(result.tempestivo()).isTrue();
        assertThat(result.semRiscoDesercao()).isTrue();
    }

    @Test
    void avaliar_comPendencias_quandoAdmissibilidadeReprovada() {
        var input = buildInput(false, LocalDate.now(), true, false, false);
        var result = service.avaliar(input);
        assertThat(result.pronto()).isFalse();
        assertThat(result.admissivel()).isFalse();
        assertThat(result.pendencias()).isNotEmpty();
    }

    @Test
    void avaliar_comPendencia_quandoDesertao() {
        var input = buildInput(true, LocalDate.now(), false, false, false);
        var result = service.avaliar(input);
        assertThat(result.semRiscoDesercao()).isFalse();
        assertThat(result.pronto()).isFalse();
    }

    private RecursoProcessualReadinessService.RecursoReadinessInput buildInput(
            boolean admissivel, LocalDate dataReferencia,
            boolean preparoLocalizado, boolean fazenda, boolean mp) {
        UUID processoId = UUID.randomUUID();
        var admInput = new RecursoAdmissibilidadeService.AdmissibilidadeInput(
                processoId, RecursoProcessualTipo.APELACAO,
                admissivel, admissivel, admissivel, admissivel,
                admissivel, admissivel, admissivel);
        var tempInput = new RecursoTempestividadeGuardService.TempestividadeInput(
                processoId, RecursoProcessualTipo.APELACAO,
                dataReferencia, fazenda, mp, false);
        var desInput = new RecursoDesertaoRadarService.DesertaoInput(
                processoId, RecursoProcessualTipo.APELACAO,
                preparoLocalizado, BigDecimal.TEN,
                false, false, false, false);
        return new RecursoProcessualReadinessService.RecursoReadinessInput(
                processoId, admInput, tempInput, desInput, cal);
    }
}
