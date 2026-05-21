package com.tcc.pjb.backend.service.penalprescricao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.penalprescricao.PenalPrescriacaoChecklistService.ClasseEtaria;
import com.tcc.pjb.backend.service.penalprescricao.PenalPrescriacaoChecklistService.FasePrescricao;
import com.tcc.pjb.backend.service.penalprescricao.PenalPrescriacaoChecklistService.PenalPrescriacaoInput;
import com.tcc.pjb.backend.service.penalprescricao.PenalPrescriacaoChecklistService.PenalPrescriacaoResult;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PenalPrescriacaoChecklistServiceTest {

    private final PenalPrescriacaoChecklistService service = new PenalPrescriacaoChecklistService();

    @ParameterizedTest(name = "penaMax={0} → prazo={1}")
    @CsvSource({
            "13.0, 20",
            "9.0,  16",
            "5.0,  12",
            "3.0,   8",
            "1.0,   4",
            "0.5,   3"
    })
    void deveCalcularPrazoPrescrionalConformalPenaMaxima(double penaMax, int prazoEsperado) {
        PenalPrescriacaoInput input = new PenalPrescriacaoInput(
                1.0, penaMax, FasePrescricao.PRETENSAO_PUNITIVA,
                ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(1), null, null, false);

        PenalPrescriacaoResult result = service.avaliar(input);

        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(prazoEsperado);
    }

    @Test
    void deveAplicarReducaoMetadeParaMenor21() {
        PenalPrescriacaoInput input = new PenalPrescriacaoInput(
                2.0, 4.0, FasePrescricao.PRETENSAO_PUNITIVA,
                ClasseEtaria.MENOR_21_ANOS_CRIME,
                LocalDate.now().minusYears(1), null, null, false);

        PenalPrescriacaoResult result = service.avaliar(input);

        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(4);
    }

    @Test
    void deveAplicarReducaoMetadeParaMaior70() {
        PenalPrescriacaoInput input = new PenalPrescriacaoInput(
                1.0, 6.0, FasePrescricao.PRETENSAO_PUNITIVA,
                ClasseEtaria.MAIOR_70_ANOS_SENTENCA,
                LocalDate.now().minusYears(1), null, null, false);

        PenalPrescriacaoResult result = service.avaliar(input);

        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(6);
    }

    @Test
    void deveRespeitarMinimoTresAnos() {
        PenalPrescriacaoInput input = new PenalPrescriacaoInput(
                0.5, 0.5, FasePrescricao.PRETENSAO_PUNITIVA,
                ClasseEtaria.MENOR_21_ANOS_CRIME,
                LocalDate.now().minusYears(1), null, null, false);

        PenalPrescriacaoResult result = service.avaliar(input);

        assertThat(result.prazoPrescriacaoAnos()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void deveIdentificarPrescricaoExpiradaQuandoVencimento() {
        PenalPrescriacaoInput input = new PenalPrescriacaoInput(
                1.0, 1.0, FasePrescricao.PRETENSAO_PUNITIVA,
                ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(10), null, null, false);

        PenalPrescriacaoResult result = service.avaliar(input);

        assertThat(result.prescricaoExpirada()).isTrue();
        assertThat(result.sinalizacao()).contains("extinção da punibilidade");
    }

    @Test
    void deveAdicionarPendenciaQuandoDataTransitoJulgadoAusenteEmExecutoria() {
        PenalPrescriacaoInput input = new PenalPrescriacaoInput(
                2.0, 4.0, FasePrescricao.PRETENSAO_EXECUTORIA,
                ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(1), null, null, false);

        PenalPrescriacaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("trânsito em julgado"));
    }

    @Test
    void deveConstruirMarcosQuandoDatasFornecidas() {
        LocalDate dataFato = LocalDate.now().minusYears(2);
        LocalDate dataUltimoAto = LocalDate.now().minusYears(1);

        PenalPrescriacaoInput input = new PenalPrescriacaoInput(
                2.0, 8.0, FasePrescricao.PRETENSAO_PUNITIVA,
                ClasseEtaria.NENHUMA,
                dataFato, null, dataUltimoAto, false);

        PenalPrescriacaoResult result = service.avaliar(input);

        assertThat(result.marcosRelevantes()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.marcosRelevantes()).anyMatch(m -> m.data().equals(dataFato));
        assertThat(result.marcosRelevantes()).anyMatch(m -> m.data().equals(dataUltimoAto));
    }

    @Test
    void deveSinalizarProximoQuandoPrescricaoMenosDe90Dias() {
        PenalPrescriacaoInput input = new PenalPrescriacaoInput(
                1.0, 1.0, FasePrescricao.PRETENSAO_PUNITIVA,
                ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(4).plusDays(60), null, null, false);

        PenalPrescriacaoResult result = service.avaliar(input);

        assertThat(result.sinalizacao()).contains("Prazo prescricional se aproxima");
    }
}
