package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Test
    void janelaComparativaChamaAnoAnteriorEAnoCorrenteDerivadosDeLocalDateNaoLiterais() {
        SalarioMinimoNacionalService salarioMock = mock(SalarioMinimoNacionalService.class);
        when(salarioMock.valorVigente()).thenReturn(new BigDecimal("9999.00"));
        when(salarioMock.valorPorAno(org.mockito.ArgumentMatchers.anyInt())).thenReturn(new BigDecimal("0.00"));
        CalculoJudicialEconomicReferenceService service = new CalculoJudicialEconomicReferenceService(salarioMock);

        int anoCorrente = LocalDate.now().getYear();

        service.current();

        verify(salarioMock).valorPorAno(anoCorrente - 1);
        verify(salarioMock).valorPorAno(anoCorrente);
    }
}
