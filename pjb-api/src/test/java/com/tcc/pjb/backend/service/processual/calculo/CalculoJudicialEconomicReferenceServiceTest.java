package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculoJudicialEconomicReferenceServiceTest {

    @Test
    void deveExporSalarioMinimoETetoInssOficiais() {
        CalculoJudicialEconomicReferenceService service = TestEconomicReferenceSupport.economicReferenceService();

        var response = service.current();

        assertThat(response.salarioMinimoNacional().referencia2026()).isEqualByComparingTo(new BigDecimal("1621.00"));
        assertThat(response.inss().tetoBeneficio2026()).isEqualByComparingTo(new BigDecimal("8475.55"));
        assertThat(response.fontesOficiais()).containsKey("salarioMinimoPlanalto2026");
    }
}
