package com.tcc.pjb.backend.service.familia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DivorcioCustodiaChecklistTest {

    private final DivorcioCustodiaChecklistService svc = new DivorcioCustodiaChecklistService();

    @Test
    void extrajudicialViavelSemFilhosESemViolencia() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.EXTRAJUDICIAL,
                DivorcioCustodiaChecklistService.SituacaoFilhos.SEM_FILHOS_MENORES,
                null,
                true, true, false, false,
                LocalDate.now().minusYears(1), false);
        var result = svc.avaliar(input);
        assertThat(result.divorcioExtrajudicialViavel()).isTrue();
        assertThat(result.pendenciasIdentificadas()).isEmpty();
    }

    @Test
    void extrajudicialInviavelComFilhosMenores() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.EXTRAJUDICIAL,
                DivorcioCustodiaChecklistService.SituacaoFilhos.COM_FILHOS_MENORES,
                DivorcioCustodiaChecklistService.ModalidadeGuarda.COMPARTILHADA,
                true, true, false, false,
                LocalDate.now().minusYears(1), false);
        var result = svc.avaliar(input);
        assertThat(result.divorcioExtrajudicialViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("733") || p.contains("menores"));
    }

    @Test
    void extrajudicialInviavelComViolenciaDomestica() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.EXTRAJUDICIAL,
                DivorcioCustodiaChecklistService.SituacaoFilhos.SEM_FILHOS_MENORES,
                null,
                true, true, false, false,
                LocalDate.now().minusYears(1), true);
        var result = svc.avaliar(input);
        assertThat(result.divorcioExtrajudicialViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("violência") || p.contains("11.340"));
    }

    @Test
    void consensualJudicialSemAcordoPatrimonialGeraPendencia() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.CONSENSUAL_JUDICIAL,
                DivorcioCustodiaChecklistService.SituacaoFilhos.SEM_FILHOS_MENORES,
                null,
                false, true, false, false,
                LocalDate.now().minusYears(1), false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("patrimonial") || p.contains("731"));
    }

    @Test
    void litigiosoInformaFomoCompetente() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.LITIGIOSO,
                DivorcioCustodiaChecklistService.SituacaoFilhos.COM_FILHOS_MENORES,
                DivorcioCustodiaChecklistService.ModalidadeGuarda.COMPARTILHADA,
                false, false, false, false,
                LocalDate.now().minusYears(2), false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("53") || o.contains("guardião") || o.contains("domicílio"));
    }

    @Test
    void guardaCompartilhadaEhRegra() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.LITIGIOSO,
                DivorcioCustodiaChecklistService.SituacaoFilhos.COM_FILHOS_MENORES,
                DivorcioCustodiaChecklistService.ModalidadeGuarda.COMPARTILHADA,
                false, false, false, false,
                LocalDate.now().minusYears(1), false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("13.058") || o.contains("compartilhada"));
    }

    @Test
    void guardaUnilateralSemViolenciaGeraPendencia() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.LITIGIOSO,
                DivorcioCustodiaChecklistService.SituacaoFilhos.COM_FILHOS_MENORES,
                DivorcioCustodiaChecklistService.ModalidadeGuarda.UNILATERAL_MAE,
                false, false, false, false,
                LocalDate.now().minusYears(1), false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("unilateral") || p.contains("1.584"));
    }

    @Test
    void filhosIncapazesExigeMP() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.LITIGIOSO,
                DivorcioCustodiaChecklistService.SituacaoFilhos.COM_FILHOS_INCAPAZES,
                DivorcioCustodiaChecklistService.ModalidadeGuarda.COMPARTILHADA,
                false, false, false, false,
                LocalDate.now().minusYears(1), false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("698") || p.contains("Ministério Público") || p.contains("curador"));
    }

    @Test
    void imovelComumSemAcordoGeraPendencia() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.CONSENSUAL_JUDICIAL,
                DivorcioCustodiaChecklistService.SituacaoFilhos.SEM_FILHOS_MENORES,
                null,
                false, true, true, false,
                LocalDate.now().minusYears(1), false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("imóvel") || p.contains("partilha"));
    }

    @Test
    void guardaAlternadaGeraPendencia() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.LITIGIOSO,
                DivorcioCustodiaChecklistService.SituacaoFilhos.COM_FILHOS_MENORES,
                DivorcioCustodiaChecklistService.ModalidadeGuarda.ALTERNADA,
                false, false, false, false,
                LocalDate.now().minusYears(1), false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("alternada") || p.contains("1.583"));
    }

    @Test
    void ec66Informado() {
        var input = new DivorcioCustodiaChecklistService.DivorcioCustodiaInput(
                DivorcioCustodiaChecklistService.ModalidadeDivorcio.CONSENSUAL_JUDICIAL,
                DivorcioCustodiaChecklistService.SituacaoFilhos.SEM_FILHOS_MENORES,
                null,
                true, true, false, false,
                LocalDate.now().minusMonths(6), false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("EC 66") || o.contains("prazo mínimo"));
    }

    @Test
    void enumModalidadeDivorcioTemTresTipos() {
        assertThat(DivorcioCustodiaChecklistService.ModalidadeDivorcio.values()).hasSize(3);
    }

    @Test
    void enumModalidadeGuardaTemQuatroTipos() {
        assertThat(DivorcioCustodiaChecklistService.ModalidadeGuarda.values()).hasSize(4);
    }
}
