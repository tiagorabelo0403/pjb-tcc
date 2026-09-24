package com.tcc.pjb.backend.service.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvEnteDevedorTipo;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TetoRpvNacionalServiceTest {

    private static final BigDecimal SALARIO_MINIMO = new BigDecimal("1518.00");
    private static final LocalDate TRANSITO = LocalDate.of(2026, 3, 10);

    private final SalarioMinimoNacionalService salarioMinimoNacionalService = salarioMinimoFixo();
    private final TetoRpvNacionalService service = new TetoRpvNacionalService(salarioMinimoNacionalService);

    private static SalarioMinimoNacionalService salarioMinimoFixo() {
        SalarioMinimoNacionalService mockado = mock(SalarioMinimoNacionalService.class);
        when(mockado.multiplicar(any(), any()))
                .thenAnswer(invocacao -> ((BigDecimal) invocacao.getArgument(0)).multiply(SALARIO_MINIMO));
        return mockado;
    }

    @ParameterizedTest
    @EnumSource(value = PrecatorioRpvEnteDevedorTipo.class,
            names = {"UNIAO", "AUTARQUIA_FEDERAL", "FUNDACAO_PUBLICA_FEDERAL"})
    void enteFederalSegueOsSessentaSalariosDaLeiDosJuizadosFederais(PrecatorioRpvEnteDevedorTipo ente) {
        assertThat(service.salariosMinimos(ente)).isEqualByComparingTo("60");
        assertThat(service.fundamentoLegal(ente)).isEqualTo("CF, art. 100, § 3º, c/c Lei 10.259/2001, art. 17, § 1º e art. 3º");
    }

    @ParameterizedTest
    @EnumSource(value = PrecatorioRpvEnteDevedorTipo.class,
            names = {"ESTADO", "DISTRITO_FEDERAL", "AUTARQUIA_ESTADUAL", "AUTARQUIA_DISTRITAL",
                     "FUNDACAO_PUBLICA_ESTADUAL", "FUNDACAO_PUBLICA_DISTRITAL"})
    void enteEstadualSegueOsQuarentaSalariosDoAdct(PrecatorioRpvEnteDevedorTipo ente) {
        assertThat(service.salariosMinimos(ente)).isEqualByComparingTo("40");
        assertThat(service.fundamentoLegal(ente)).isEqualTo("CF, art. 100, § 3º, c/c ADCT, art. 87, I");
    }

    @ParameterizedTest
    @EnumSource(value = PrecatorioRpvEnteDevedorTipo.class,
            names = {"MUNICIPIO", "AUTARQUIA_MUNICIPAL", "FUNDACAO_PUBLICA_MUNICIPAL"})
    void enteMunicipalSegueOsTrintaSalariosDoAdct(PrecatorioRpvEnteDevedorTipo ente) {
        assertThat(service.salariosMinimos(ente)).isEqualByComparingTo("30");
        assertThat(service.fundamentoLegal(ente)).isEqualTo("CF, art. 100, § 3º, c/c ADCT, art. 87, II");
    }

    @Test
    void limiteEmDinheiroMultiplicaPeloSalarioMinimoDoTransitoEmJulgado() {
        assertThat(service.limite(PrecatorioRpvEnteDevedorTipo.UNIAO, TRANSITO))
                .isEqualByComparingTo(SALARIO_MINIMO.multiply(new BigDecimal("60")));
        assertThat(service.limite(PrecatorioRpvEnteDevedorTipo.ESTADO, TRANSITO))
                .isEqualByComparingTo(SALARIO_MINIMO.multiply(new BigDecimal("40")));
        assertThat(service.limite(PrecatorioRpvEnteDevedorTipo.MUNICIPIO, TRANSITO))
                .isEqualByComparingTo(SALARIO_MINIMO.multiply(new BigDecimal("30")));
        verify(salarioMinimoNacionalService).multiplicar(new BigDecimal("60"), TRANSITO);
        verify(salarioMinimoNacionalService).multiplicar(new BigDecimal("40"), TRANSITO);
        verify(salarioMinimoNacionalService).multiplicar(new BigDecimal("30"), TRANSITO);
    }

    @Test
    void enteOuDataAusenteFalhaEmVezDePresumirTeto() {
        assertThatThrownBy(() -> service.salariosMinimos(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.limite(PrecatorioRpvEnteDevedorTipo.UNIAO, null))
                .isInstanceOf(NullPointerException.class);
    }
}
