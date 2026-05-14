package com.tcc.pjb.backend.service.custas;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustasProcessuaisCalculatorServiceTest {

    private final CustasProcessuaisCalculatorService service =
            new CustasProcessuaisCalculatorService();

    @Test
    void deveIsentarBeneficiarioDeJusticaGratuita() {
        CustasProcessuaisCalculatorService.CustasInput input =
                new CustasProcessuaisCalculatorService.CustasInput(
                        UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                        new BigDecimal("10000"), TipoCusta.PREPARO_RECURSAL,
                        true, false, false, "SP");
        CustasProcessuaisCalculatorService.CustasCalculadas calc = service.calcular(input);
        assertThat(calc.isento()).isTrue();
        assertThat(calc.valorFinal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deveIsentarFazendaPublica() {
        CustasProcessuaisCalculatorService.CustasInput input =
                new CustasProcessuaisCalculatorService.CustasInput(
                        UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                        new BigDecimal("50000"), TipoCusta.PREPARO_RECURSAL,
                        false, true, false, "RJ");
        CustasProcessuaisCalculatorService.CustasCalculadas calc = service.calcular(input);
        assertThat(calc.isento()).isTrue();
    }

    @Test
    void deveCalcularPreparoRecursal2Porcento() {
        CustasProcessuaisCalculatorService.CustasInput input =
                new CustasProcessuaisCalculatorService.CustasInput(
                        UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                        new BigDecimal("10000"), TipoCusta.PREPARO_RECURSAL,
                        false, false, false, "SP");
        CustasProcessuaisCalculatorService.CustasCalculadas calc = service.calcular(input);
        assertThat(calc.isento()).isFalse();
        assertThat(calc.valorFinal()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void deveAdicionarAlertaParaMultaQueRequerDespacho() {
        CustasProcessuaisCalculatorService.CustasInput input =
                new CustasProcessuaisCalculatorService.CustasInput(
                        UUID.randomUUID(), RitoProcessual.PROCEDIMENTO_COMUM,
                        new BigDecimal("5000"), TipoCusta.MULTA_LITIGANCIA_MA_FE,
                        false, false, false, "MG");
        CustasProcessuaisCalculatorService.CustasCalculadas calc = service.calcular(input);
        assertThat(calc.alertas()).anyMatch(a -> a.contains("despacho jurisdicional"));
    }
}
