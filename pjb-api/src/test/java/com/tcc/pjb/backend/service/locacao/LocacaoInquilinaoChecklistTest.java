package com.tcc.pjb.backend.service.locacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocacaoInquilinaoChecklistTest {

    private final LocacaoInquilinaoChecklistService svc = new LocacaoInquilinaoChecklistService();

    @Test
    void locacaoResidencialSemPendencias() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.RESIDENCIAL,
                LocacaoInquilinaoChecklistService.SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(12),
                LocalDate.now().plusMonths(12),
                new BigDecimal("1500.00"),
                0, true, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("advogado").isNotBlank();
    }

    @Test
    void inadimplenciaGeraPendenciaComEstimativaDebito() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.RESIDENCIAL,
                LocacaoInquilinaoChecklistService.SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(24),
                LocalDate.now().plusMonths(6),
                new BigDecimal("2000.00"),
                3, true, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("inadimplência") || p.contains("mês"));
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("despejo") || o.fundamentoLegal().contains("9º"));
    }

    @Test
    void locacaoResidencialComEstabilidade30MesesSinalizaDespejo() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.RESIDENCIAL,
                LocacaoInquilinaoChecklistService.SituacaoContrato.VENCIDO_RENOVANDO,
                LocalDate.now().minusMonths(37),
                LocalDate.now().minusMonths(6),
                new BigDecimal("1800.00"),
                0, true, true, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("estabilidade") || p.contains("30 meses") || p.contains("denúncia"));
    }

    @Test
    void renovacaoCompulsoriaComercialEligivelApos5Anos() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.COMERCIAL,
                LocacaoInquilinaoChecklistService.SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(62),
                LocalDate.now().plusMonths(2),
                new BigDecimal("5000.00"),
                0, true, false, true);
        var result = svc.avaliar(input);
        assertThat(result.renovacaoCompulsoriaEligivel()).isTrue();
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("renovatória") || o.fundamentoLegal().contains("51"));
    }

    @Test
    void renovacaoCompulsoriaNaoEligivelContratoCurto() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.COMERCIAL,
                LocacaoInquilinaoChecklistService.SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(24),
                LocalDate.now().plusMonths(6),
                new BigDecimal("3000.00"),
                0, true, false, true);
        var result = svc.avaliar(input);
        assertThat(result.renovacaoCompulsoriaEligivel()).isFalse();
    }

    @Test
    void semGarantiaIndicaOrientacao() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.RESIDENCIAL,
                LocacaoInquilinaoChecklistService.SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusMonths(18),
                new BigDecimal("1200.00"),
                0, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("garantia") || o.fundamentoLegal().contains("37"));
    }

    @Test
    void locacaoTemporadaIndicaLimiteDe90Dias() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.TEMPORADA,
                LocacaoInquilinaoChecklistService.SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(60),
                new BigDecimal("3500.00"),
                0, true, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("90 dias") || o.fundamentoLegal().contains("48"));
    }

    @Test
    void contratoVencidoSinalizaNotificacaoDe30Dias() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.RESIDENCIAL,
                LocacaoInquilinaoChecklistService.SituacaoContrato.VENCIDO_RENOVANDO,
                LocalDate.now().minusMonths(18),
                LocalDate.now().minusMonths(6),
                new BigDecimal("1600.00"),
                0, true, true, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoDespejoCurto()).isTrue();
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("30 dias") || o.fundamentoLegal().contains("46"));
    }

    @Test
    void semDataInicioGeraPendencia() {
        var input = new LocacaoInquilinaoChecklistService.LocacaoInput(
                LocacaoInquilinaoChecklistService.TipoLocacao.RESIDENCIAL,
                LocacaoInquilinaoChecklistService.SituacaoContrato.EM_VIGOR,
                null,
                LocalDate.now().plusMonths(12),
                new BigDecimal("1400.00"),
                0, true, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("data") || p.contains("início"));
    }

    @Test
    void enumTipoLocacaoTemTresModalidades() {
        assertThat(LocacaoInquilinaoChecklistService.TipoLocacao.values()).hasSize(3);
    }
}
