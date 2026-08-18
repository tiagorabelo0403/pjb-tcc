package com.tcc.pjb.backend.service.execucaopenal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.execucaopenal.PenaPrivativaCalculatorService.ConcursoTipo;
import com.tcc.pjb.backend.service.execucaopenal.PenaPrivativaCalculatorService.PenaInput;
import com.tcc.pjb.backend.service.execucaopenal.PenaPrivativaCalculatorService.PenaUnificada;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PenaPrivativaCalculatorServiceTest {

    private final PenaPrivativaCalculatorService service = new PenaPrivativaCalculatorService();

    @Test
    void deveUnificarPorSomaParaConcursoMaterial() {
        PenaInput input = new PenaInput(List.of(24, 36), ConcursoTipo.MATERIAL, false);

        PenaUnificada result = service.unificar(input);

        assertThat(result.totalMeses()).isEqualTo(60);
        assertThat(result.fundamentacao()).contains("material");
    }

    @Test
    void deveAplicarUmSextoParaFormalProprio() {
        PenaInput input = new PenaInput(List.of(36, 24), ConcursoTipo.FORMAL_PROPRIO, false);

        PenaUnificada result = service.unificar(input);

        int adicional = (int) Math.ceil(36.0 / 6);
        assertThat(result.totalMeses()).isEqualTo(36 + adicional);
        assertThat(result.fundamentacao()).contains("formal próprio");
    }

    @Test
    void deveUnificarPorSomaParaFormalImproprio() {
        PenaInput input = new PenaInput(List.of(12, 18), ConcursoTipo.FORMAL_IMPROPRIO, false);

        PenaUnificada result = service.unificar(input);

        assertThat(result.totalMeses()).isEqualTo(30);
    }

    @Test
    void deveAplicarDoisTercosParaContinuado() {
        PenaInput input = new PenaInput(List.of(30, 20), ConcursoTipo.CONTINUADO, false);

        PenaUnificada result = service.unificar(input);

        int adicional = (int) Math.ceil(30 * 2.0 / 3);
        assertThat(result.totalMeses()).isEqualTo(30 + adicional);
        assertThat(result.fundamentacao()).contains("continuado");
    }

    @Test
    void deveTriplilarParaContinuadoEspecial() {
        PenaInput input = new PenaInput(List.of(24, 18), ConcursoTipo.CONTINUADO_ESPECIAL, false);

        PenaUnificada result = service.unificar(input);

        assertThat(result.totalMeses()).isEqualTo(24 * 3);
    }

    @Test
    void deveRetornarAlertaQuandoPenaSuperior40Anos() {
        PenaInput input = new PenaInput(List.of(300, 300), ConcursoTipo.MATERIAL, false);

        PenaUnificada result = service.unificar(input);

        assertThat(result.alertas()).anyMatch(a -> a.contains("40 anos"));
    }

    @Test
    void deveRetornarZeroParaListaVazia() {
        PenaInput input = new PenaInput(List.of(), ConcursoTipo.MATERIAL, false);

        PenaUnificada result = service.unificar(input);

        assertThat(result.totalMeses()).isEqualTo(0);
    }

    @ParameterizedTest(name = "concurso={0}")
    @CsvSource({
            "MATERIAL",
            "FORMAL_PROPRIO",
            "FORMAL_IMPROPRIO",
            "CONTINUADO",
            "CONTINUADO_ESPECIAL"
    })
    void deveRetornarFundamentacaoParaTodosTiposConcurso(ConcursoTipo concurso) {
        PenaInput input = new PenaInput(List.of(12, 6), concurso, false);

        PenaUnificada result = service.unificar(input);

        assertThat(result.fundamentacao()).isNotBlank();
    }
}
