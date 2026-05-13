package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalculoJudicialEconomicReferenceServiceTest {

    @Test
    void deveExporSalarioMinimoETetoInssOficiais() {
        CalculoJudicialEconomicReferenceService service = TestEconomicReferenceSupport.economicReferenceService();

        var response = service.current();

        assertThat(response.salarioMinimoNacional()).containsEntry("referencia2026", new java.math.BigDecimal("1621.00"));
        assertThat(response.inss()).containsEntry("tetoBeneficio2026", new java.math.BigDecimal("8475.55"));
        assertThat(response.fontesOficiais()).containsKey("salarioMinimoPlanalto2026");
    }
}
