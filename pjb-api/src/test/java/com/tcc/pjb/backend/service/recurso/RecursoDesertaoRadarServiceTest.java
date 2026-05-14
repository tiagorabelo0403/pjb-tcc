package com.tcc.pjb.backend.service.recurso;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecursoDesertaoRadarServiceTest {

    private final RecursoDesertaoRadarService service = new RecursoDesertaoRadarService();

    @Test
    void analisar_isento_quandoEmbargosDeclaracao() {
        var input = input(RecursoProcessualTipo.EMBARGOS_DECLARACAO,
                false, BigDecimal.ZERO, false, false, false, false);
        var result = service.analisar(input);
        assertThat(result.isento()).isTrue();
        assertThat(result.emRiscoDeDesercao()).isFalse();
        assertThat(result.motivoIsencao()).contains("art. 1.007");
    }

    @Test
    void analisar_isento_quandoFazendaPublica() {
        var input = input(RecursoProcessualTipo.APELACAO,
                false, BigDecimal.TEN, false, true, false, false);
        var result = service.analisar(input);
        assertThat(result.isento()).isTrue();
        assertThat(result.emRiscoDeDesercao()).isFalse();
    }

    @Test
    void analisar_isento_quandoMinisterioPublico() {
        var input = input(RecursoProcessualTipo.APELACAO,
                false, BigDecimal.TEN, false, false, true, false);
        var result = service.analisar(input);
        assertThat(result.isento()).isTrue();
    }

    @Test
    void analisar_isento_quandoGratuidade() {
        var input = input(RecursoProcessualTipo.APELACAO,
                false, BigDecimal.TEN, true, false, false, false);
        var result = service.analisar(input);
        assertThat(result.isento()).isTrue();
        assertThat(result.motivoIsencao()).contains("gratuidade");
    }

    @Test
    void analisar_semRisco_quandoPreparoLocalizado() {
        var input = input(RecursoProcessualTipo.APELACAO,
                true, BigDecimal.TEN, false, false, false, false);
        var result = service.analisar(input);
        assertThat(result.emRiscoDeDesercao()).isFalse();
        assertThat(result.isento()).isFalse();
    }

    @Test
    void analisar_emRisco_quandoSemPreparoESemIsencao() {
        var input = input(RecursoProcessualTipo.APELACAO,
                false, BigDecimal.ZERO, false, false, false, false);
        var result = service.analisar(input);
        assertThat(result.emRiscoDeDesercao()).isTrue();
        assertThat(result.diagnostico()).contains("art. 1.007");
    }

    @Test
    void analisar_isento_quandoDefensoria() {
        var input = input(RecursoProcessualTipo.APELACAO,
                false, BigDecimal.ZERO, false, false, false, true);
        var result = service.analisar(input);
        assertThat(result.isento()).isTrue();
    }

    private RecursoDesertaoRadarService.DesertaoInput input(
            RecursoProcessualTipo tipo, boolean preparoLocalizado, BigDecimal valorPreparo,
            boolean gratuidade, boolean fazenda, boolean mp, boolean defensoria) {
        return new RecursoDesertaoRadarService.DesertaoInput(
                UUID.randomUUID(), tipo, preparoLocalizado, valorPreparo,
                gratuidade, fazenda, mp, defensoria);
    }
}
