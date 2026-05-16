package com.tcc.pjb.backend.service.penalprescricao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PenalPrescriacaoChecklistTest {

    private final PenalPrescriacaoChecklistService svc = new PenalPrescriacaoChecklistService();

    @Test
    void penaMenorQue1AnoPrescreve3Anos() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                0.25, 0.5,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(1),
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(3);
        assertThat(result.prescricaoExpirada()).isFalse();
    }

    @Test
    void penaMaxima2AnosPrescreve4Anos() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                1.0, 2.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(5),
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(4);
        assertThat(result.prescricaoExpirada()).isTrue();
    }

    @Test
    void penaMaxima4AnosPrescreve8Anos() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                2.0, 4.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(5),
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(8);
        assertThat(result.prescricaoExpirada()).isFalse();
    }

    @Test
    void menor21AnosReduzPrazoMetade() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                2.0, 4.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.MENOR_21_ANOS_CRIME,
                LocalDate.now().minusYears(5),
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(4);
        assertThat(result.prescricaoExpirada()).isTrue();
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("21") || o.contains("115"));
    }

    @Test
    void maior70AnosSentencaReduzPrazoMetade() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                2.0, 8.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.MAIOR_70_ANOS_SENTENCA,
                LocalDate.now().minusYears(3),
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(6);
        assertThat(result.prescricaoExpirada()).isFalse();
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("70") || o.contains("115"));
    }

    @Test
    void atoCausaInterruptivaReiniciaContagem() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                2.0, 4.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(10),
                null,
                LocalDate.now().minusYears(2),
                false);
        var result = svc.avaliar(input);
        assertThat(result.prescricaoExpirada()).isFalse();
    }

    @Test
    void pretensaoExecutoriaUsaDataTransito() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                2.0, 4.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_EXECUTORIA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(10),
                LocalDate.now().minusYears(3),
                null, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(4);
        assertThat(result.prescricaoExpirada()).isFalse();
        assertThat(result.marcosRelevantes()).anyMatch(m -> m.fundamentoLegal().contains("112"));
    }

    @Test
    void semDataFatoGeraPendencia() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                2.0, 4.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                null,
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("data") || p.contains("fato"));
    }

    @Test
    void pretensaoExecutoriaSemTransitoGeraPendencia() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                2.0, 4.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_EXECUTORIA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(5),
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("trânsito") || p.contains("executória"));
    }

    @Test
    void penaMaxima12AnosPrescreve20Anos() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                6.0, 14.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(5),
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoPrescriacaoAnos()).isEqualTo(20);
        assertThat(result.prescricaoExpirada()).isFalse();
    }

    @Test
    void orientacoesInformamCP117() {
        var input = new PenalPrescriacaoChecklistService.PenalPrescriacaoInput(
                1.0, 4.0,
                PenalPrescriacaoChecklistService.FasePrescricao.PRETENSAO_PUNITIVA,
                PenalPrescriacaoChecklistService.ClasseEtaria.NENHUMA,
                LocalDate.now().minusYears(2),
                null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("117") || o.contains("interruptivas"));
    }

    @Test
    void enumFasePrescricaoTemDuasModalidades() {
        assertThat(PenalPrescriacaoChecklistService.FasePrescricao.values()).hasSize(2);
    }

    @Test
    void enumClasseEtariaTemTresModalidades() {
        assertThat(PenalPrescriacaoChecklistService.ClasseEtaria.values()).hasSize(3);
    }
}
