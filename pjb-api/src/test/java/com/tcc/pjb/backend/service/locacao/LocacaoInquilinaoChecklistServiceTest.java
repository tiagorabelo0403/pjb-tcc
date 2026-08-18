package com.tcc.pjb.backend.service.locacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.locacao.LocacaoInquilinaoChecklistService.LocacaoInput;
import com.tcc.pjb.backend.service.locacao.LocacaoInquilinaoChecklistService.LocacaoResult;
import com.tcc.pjb.backend.service.locacao.LocacaoInquilinaoChecklistService.SituacaoContrato;
import com.tcc.pjb.backend.service.locacao.LocacaoInquilinaoChecklistService.TipoLocacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocacaoInquilinaoChecklistServiceTest {

    private final LocacaoInquilinaoChecklistService service = new LocacaoInquilinaoChecklistService();

    @Test
    void deveIndicarRenovatoriaParaComercialComMaisDe60Meses() {
        LocacaoInput input = new LocacaoInput(
                TipoLocacao.COMERCIAL, SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(70), null,
                new BigDecimal("3000.00"), 0, true, false, true);

        LocacaoResult result = service.avaliar(input);

        assertThat(result.renovacaoCompulsoriaEligivel()).isTrue();
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.fundamentoLegal().contains("51"));
    }

    @Test
    void deveNaoIndicarRenovatoriaParaComercialMenos60Meses() {
        LocacaoInput input = new LocacaoInput(
                TipoLocacao.COMERCIAL, SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(30), null,
                new BigDecimal("2000.00"), 0, true, false, true);

        LocacaoResult result = service.avaliar(input);

        assertThat(result.renovacaoCompulsoriaEligivel()).isFalse();
    }

    @Test
    void deveAdicionarPendenciaInadimplencia() {
        LocacaoInput input = new LocacaoInput(
                TipoLocacao.RESIDENCIAL, SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(12), null,
                new BigDecimal("1500.00"), 3, true, false, false);

        LocacaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("3 mês"));
    }

    @Test
    void deveAdicionarOrientacaoGarantiaAusente() {
        LocacaoInput input = new LocacaoInput(
                TipoLocacao.RESIDENCIAL, SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(6), null,
                new BigDecimal("1200.00"), 0, false, false, false);

        LocacaoResult result = service.avaliar(input);

        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.fundamentoLegal().contains("37"));
    }

    @Test
    void deveAdicionarPendenciaEstabilidadeResidencial30Meses() {
        LocacaoInput input = new LocacaoInput(
                TipoLocacao.RESIDENCIAL, SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusMonths(35), null,
                new BigDecimal("900.00"), 0, true, true, false);

        LocacaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("30 meses"));
    }

    @Test
    void deveAdicionarOrientacaoTemporadaMaximo90Dias() {
        LocacaoInput input = new LocacaoInput(
                TipoLocacao.TEMPORADA, SituacaoContrato.EM_VIGOR,
                LocalDate.now().minusDays(20), LocalDate.now().plusDays(40),
                new BigDecimal("500.00"), 0, true, false, false);

        LocacaoResult result = service.avaliar(input);

        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.fundamentoLegal().contains("48"));
    }

    @Test
    void deveAdicionarPendenciaDataInicioNaoInformada() {
        LocacaoInput input = new LocacaoInput(
                TipoLocacao.RESIDENCIAL, SituacaoContrato.EM_VIGOR,
                null, null,
                new BigDecimal("800.00"), 0, true, false, false);

        LocacaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("data de início"));
    }
}
