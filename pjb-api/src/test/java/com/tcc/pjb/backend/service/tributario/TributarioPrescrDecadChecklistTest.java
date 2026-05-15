package com.tcc.pjb.backend.service.tributario;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TributarioPrescrDecadChecklistTest {

    private final TributarioPrescrDecadChecklistService svc = new TributarioPrescrDecadChecklistService();

    @Test
    void decadenciaHomologacaoNaoExpiradaFatoGeradorRecente() {
        var input = new TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput(
                "12.345.678/0001-00",
                LocalDate.now().minusYears(2),
                null, null,
                TributarioPrescrDecadChecklistService.ModalidadeLancamento.HOMOLOGACAO,
                new BigDecimal("5000.00"),
                false);
        var result = svc.avaliar(input);
        assertThat(result.decadenciaIdentificada()).isFalse();
        assertThat(result.diasRestantesDecadencia()).isGreaterThan(0);
        assertThat(result.pendenciasIdentificadas()).noneMatch(p -> p.contains("decadência") || p.contains("decadencial"));
    }

    @Test
    void decadenciaHomologacaoExpiradaFatoGeradorSeisAnos() {
        var input = new TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput(
                "12.345.678/0001-01",
                LocalDate.now().minusYears(6),
                null, null,
                TributarioPrescrDecadChecklistService.ModalidadeLancamento.HOMOLOGACAO,
                new BigDecimal("8000.00"),
                false);
        var result = svc.avaliar(input);
        assertThat(result.decadenciaIdentificada()).isTrue();
        assertThat(result.diasRestantesDecadencia()).isEqualTo(0);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("decadência") || p.contains("decadencial"));
    }

    @Test
    void decadenciaOficioContaDesde1JaneiroDoExercicioSeguinte() {
        var input = new TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput(
                "12.345.678/0001-02",
                LocalDate.now().minusYears(2),
                null, null,
                TributarioPrescrDecadChecklistService.ModalidadeLancamento.OFICIO,
                new BigDecimal("3000.00"),
                false);
        var result = svc.avaliar(input);
        assertThat(result.decadenciaIdentificada()).isFalse();
        assertThat(result.diasRestantesDecadencia()).isGreaterThan(0);
        assertThat(result.requisitosVerificados()).anyMatch(r -> r.contains("decadencial") || r.contains("decadência"));
    }

    @Test
    void prescricaoExpiradaConstituicaoDefinitivaHaSeisAnos() {
        var input = new TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput(
                "12.345.678/0001-03",
                LocalDate.now().minusYears(8),
                LocalDate.now().minusYears(6),
                null,
                TributarioPrescrDecadChecklistService.ModalidadeLancamento.HOMOLOGACAO,
                new BigDecimal("10000.00"),
                false);
        var result = svc.avaliar(input);
        assertThat(result.prescricaoIdentificada()).isTrue();
        assertThat(result.diasRestantesPrescricao()).isEqualTo(0);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("prescrição") || p.contains("prescricional"));
    }

    @Test
    void prescricaoInterrompidaRecontaDesdeAtoCausativo() {
        var input = new TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput(
                "12.345.678/0001-04",
                LocalDate.now().minusYears(8),
                LocalDate.now().minusYears(6),
                LocalDate.now().minusYears(2),
                TributarioPrescrDecadChecklistService.ModalidadeLancamento.HOMOLOGACAO,
                new BigDecimal("10000.00"),
                true);
        var result = svc.avaliar(input);
        assertThat(result.prescricaoIdentificada()).isFalse();
        assertThat(result.diasRestantesPrescricao()).isGreaterThan(0);
        assertThat(result.requisitosVerificados()).anyMatch(r -> r.contains("interruptiva") || r.contains("interrompido"));
    }

    @Test
    void semFatoGeradorGeraPendenciaDecadencial() {
        var input = new TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput(
                "12.345.678/0001-05",
                null,
                null, null,
                TributarioPrescrDecadChecklistService.ModalidadeLancamento.OFICIO,
                new BigDecimal("2000.00"),
                false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("fato gerador"));
    }

    @Test
    void execucaoFiscalSemConstituicaoDefinitivaGeraPendencia() {
        var input = new TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput(
                "12.345.678/0001-06",
                LocalDate.now().minusYears(3),
                null, null,
                TributarioPrescrDecadChecklistService.ModalidadeLancamento.DECLARACAO,
                new BigDecimal("15000.00"),
                true);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("constituição") || p.contains("execução fiscal"));
    }

    @Test
    void execucaoFiscalAjuizadaIndicaRequisitoDeCitacao() {
        var input = new TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput(
                "12.345.678/0001-07",
                LocalDate.now().minusYears(3),
                LocalDate.now().minusYears(1),
                null,
                TributarioPrescrDecadChecklistService.ModalidadeLancamento.DECLARACAO,
                new BigDecimal("7000.00"),
                true);
        var result = svc.avaliar(input);
        assertThat(result.requisitosIndicados()).anyMatch(r ->
                r.descricao().contains("Execução fiscal") || r.fundamentoLegal().contains("6.830"));
    }

    @Test
    void enumModalidadeLancamentoTemTresOpcoes() {
        assertThat(TributarioPrescrDecadChecklistService.ModalidadeLancamento.values()).hasSize(3);
    }
}
