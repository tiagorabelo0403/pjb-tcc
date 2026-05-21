package com.tcc.pjb.backend.service.processual.execucao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.execucao.CumprimentoSentencaReadinessService.CumprimentoInput;
import com.tcc.pjb.backend.service.processual.execucao.CumprimentoSentencaReadinessService.CumprimentoSnapshot;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CumprimentoSentencaReadinessServiceTest {

    private final CumprimentoSentencaReadinessService service = new CumprimentoSentencaReadinessService();

    @Test
    void deveRetornarAptaQuandoTodosPressupostosAtendidos() {
        CumprimentoInput input = new CumprimentoInput(
                UUID.randomUUID(), new BigDecimal("50000.00"),
                true, true, false, true, true, false, false);

        CumprimentoSnapshot snap = service.avaliar(input);

        assertThat(snap.aptaParaCumprimento()).isTrue();
        assertThat(snap.pendencias()).isEmpty();
    }

    @Test
    void deveAdicionarPendenciaQuandoSemTransito() {
        CumprimentoInput input = new CumprimentoInput(
                UUID.randomUUID(), new BigDecimal("10000.00"),
                true, false, false, false, false, false, false);

        CumprimentoSnapshot snap = service.avaliar(input);

        assertThat(snap.aptaParaCumprimento()).isFalse();
        assertThat(snap.pendencias()).anyMatch(p -> p.contains("trânsito em julgado"));
    }

    @Test
    void deveAdicionarPendenciaLiquidacaoQuandoValorNuloESemLiquidacao() {
        CumprimentoInput input = new CumprimentoInput(
                UUID.randomUUID(), null,
                false, true, false, false, false, false, false);

        CumprimentoSnapshot snap = service.avaliar(input);

        assertThat(snap.pendencias()).anyMatch(p -> p.contains("Liquidação"));
    }

    @Test
    void deveAdicionarEtapaIntimacaoQuandoTransitadoSemIntimacaoDevedor() {
        CumprimentoInput input = new CumprimentoInput(
                UUID.randomUUID(), new BigDecimal("20000.00"),
                true, true, false, false, false, false, false);

        CumprimentoSnapshot snap = service.avaliar(input);

        assertThat(snap.etapas()).anyMatch(e -> e.contains("Intimar devedor"));
    }

    @Test
    void deveCalcularMulta10PorcentoQuandoDevedorIntimadoSemPagamento() {
        BigDecimal valorCondenacao = new BigDecimal("30000.00");
        CumprimentoInput input = new CumprimentoInput(
                UUID.randomUUID(), valorCondenacao,
                true, true, false, true, false, false, false);

        CumprimentoSnapshot snap = service.avaliar(input);

        BigDecimal esperada = valorCondenacao.multiply(new BigDecimal("0.10"));
        assertThat(snap.multaDevida()).isEqualByComparingTo(esperada);
        assertThat(snap.etapas()).anyMatch(e -> e.contains("10%"));
    }

    @Test
    void deveRetornarMultaZeroQuandoPrazoVoluntarioPago() {
        CumprimentoInput input = new CumprimentoInput(
                UUID.randomUUID(), new BigDecimal("20000.00"),
                true, true, false, true, true, false, false);

        CumprimentoSnapshot snap = service.avaliar(input);

        assertThat(snap.multaDevida()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest(name = "transitado={0} intimado={1} pago={2} → apta={3}")
    @CsvSource({
            "true,  true,  true,  true",
            "false, false, false, false",
            "true,  false, false, true"
    })
    void deveResolverAptidaoConformalEstadoDoProcesso(
            boolean transitado, boolean intimado, boolean pago, boolean expectedApta) {
        CumprimentoInput input = new CumprimentoInput(
                UUID.randomUUID(), new BigDecimal("15000.00"),
                true, transitado, false, intimado, pago, false, false);

        CumprimentoSnapshot snap = service.avaliar(input);

        assertThat(snap.aptaParaCumprimento()).isEqualTo(expectedApta);
    }
}
