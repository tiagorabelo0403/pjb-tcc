package com.tcc.pjb.backend.service.tributario;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.tributario.TributarioPrescrDecadChecklistService.ModalidadeLancamento;
import com.tcc.pjb.backend.service.tributario.TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput;
import com.tcc.pjb.backend.service.tributario.TributarioPrescrDecadChecklistService.TributarioPrescrDecadResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TributarioPrescrDecadChecklistServiceTest {

    private final TributarioPrescrDecadChecklistService service = new TributarioPrescrDecadChecklistService();

    @Test
    void deveDetectarDecadenciaParaFatoGeradorHaMaisDe5Anos() {
        TributarioPrescrDecadInput input = new TributarioPrescrDecadInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(6),
                null, null,
                ModalidadeLancamento.OFICIO,
                new BigDecimal("10000.00"), false);

        TributarioPrescrDecadResult result = service.avaliar(input);

        assertThat(result.decadenciaIdentificada()).isTrue();
        assertThat(result.diasRestantesDecadencia()).isEqualTo(0);
    }

    @Test
    void deveNaoDetectarDecadenciaParaFatoGeradorRecente() {
        TributarioPrescrDecadInput input = new TributarioPrescrDecadInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(2),
                null, null,
                ModalidadeLancamento.HOMOLOGACAO,
                new BigDecimal("5000.00"), false);

        TributarioPrescrDecadResult result = service.avaliar(input);

        assertThat(result.decadenciaIdentificada()).isFalse();
        assertThat(result.diasRestantesDecadencia()).isPositive();
    }

    @Test
    void deveDetectarPrescricaoQuandoConstituicaoDefinitivaHaMaisDe5Anos() {
        TributarioPrescrDecadInput input = new TributarioPrescrDecadInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(7),
                LocalDate.now().minusYears(6),
                null,
                ModalidadeLancamento.DECLARACAO,
                new BigDecimal("20000.00"), true);

        TributarioPrescrDecadResult result = service.avaliar(input);

        assertThat(result.prescricaoIdentificada()).isTrue();
    }

    @Test
    void deveAdicionarPendenciaQuandoSemDataFatoGerador() {
        TributarioPrescrDecadInput input = new TributarioPrescrDecadInput(
                "12.345.678/0001-00",
                null, null, null,
                ModalidadeLancamento.OFICIO,
                new BigDecimal("1000.00"), false);

        TributarioPrescrDecadResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("fato gerador") || p.contains("data"));
    }

    @Test
    void deveReiniciarPrazoComAtoCausaInterruptiva() {
        TributarioPrescrDecadInput input = new TributarioPrescrDecadInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(8),
                LocalDate.now().minusYears(8),
                LocalDate.now().minusYears(2),
                ModalidadeLancamento.OFICIO,
                new BigDecimal("15000.00"), true);

        TributarioPrescrDecadResult result = service.avaliar(input);

        assertThat(result.prescricaoIdentificada()).isFalse();
        assertThat(result.requisitosVerificados()).anyMatch(v -> v.contains("interruptiva") || v.contains("Interruptiva"));
    }
}
