package com.tcc.pjb.backend.platform.jusos.v2.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalRulePackEngineTest {

    @Test
    void shouldTrimCustomBucketsRegistry() throws Exception {
        NationalRulePackEngine engine = new NationalRulePackEngine(mock(SalarioMinimoNacionalService.class));

        for (int i = 0; i < 320; i++) {
            engine.registrarRegraCustomizada(
                    "TJ" + i,
                    RamoDireito.CIVIL,
                    new NationalRulePackEngine.RegraAlerta("COD_" + i, "desc", RamoDireito.CIVIL, "alerta", "INFO")
            );
        }

        Field customField = NationalRulePackEngine.class.getDeclaredField("regrasCustomizadas");
        customField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> custom = (Map<String, ?>) customField.get(engine);

        Field touchField = NationalRulePackEngine.class.getDeclaredField("bucketTouch");
        touchField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> touch = (Map<String, ?>) touchField.get(engine);

        assertThat(custom).hasSizeLessThanOrEqualTo(256);
        assertThat(touch).hasSizeLessThanOrEqualTo(256);
    }

    private static final LocalDate DISTRIBUICAO = LocalDate.of(2024, 5, 20);

    private static NationalRulePackEngine engineComSalarioDe(String valor) {
        SalarioMinimoNacionalService servico = mock(SalarioMinimoNacionalService.class);
        when(servico.multiplicar(any(BigDecimal.class), any(LocalDate.class)))
                .thenAnswer(inv -> inv.<BigDecimal>getArgument(0).multiply(new BigDecimal(valor)));
        return new NationalRulePackEngine(servico);
    }

    private static NationalRulePackEngine.ContextoRegra contextoCivil(Map<String, Object> extras) {
        return new NationalRulePackEngine.ContextoRegra(
                "PROCEDIMENTO COMUM", "Contratos", RamoDireito.CIVIL, null, "TJCE", extras);
    }

    @Test
    void competenciaDoJuizadoUsaOSalarioMinimoDaDataDeReferencia() {
        SalarioMinimoNacionalService servico = mock(SalarioMinimoNacionalService.class);
        when(servico.multiplicar(any(BigDecimal.class), any(LocalDate.class)))
                .thenAnswer(inv -> inv.<BigDecimal>getArgument(0).multiply(new BigDecimal("1412.00")));
        NationalRulePackEngine engine = new NationalRulePackEngine(servico);

        var resultado = engine.aplicar(contextoCivil(Map.of(
                "valorCausa", new BigDecimal("50000.00"),
                "dataReferencia", DISTRIBUICAO)));

        assertThat(resultado.aplicadas())
                .as("40 x 1412,00 = 56.480,00, entao causa de 50.000,00 cabe no rito do Juizado")
                .anyMatch(regra -> "JEC_COMPETENCIA_POTENCIAL".equals(regra.codigo()));
        verify(servico).multiplicar(eq(new BigDecimal("40")), eq(DISTRIBUICAO));
    }

    @Test
    void mesmoValorDeCausaMudaDeRespostaConformeOSalarioMinimoDaEpoca() {
        var extras = Map.<String, Object>of(
                "valorCausa", new BigDecimal("60000.00"),
                "dataReferencia", DISTRIBUICAO);

        var com2024 = engineComSalarioDe("1412.00").aplicar(contextoCivil(extras));
        var com2026 = engineComSalarioDe("1621.00").aplicar(contextoCivil(extras));

        assertThat(com2024.aplicadas())
                .as("40 x 1412,00 = 56.480,00: causa de 60.000,00 fica fora do Juizado")
                .noneMatch(regra -> "JEC_COMPETENCIA_POTENCIAL".equals(regra.codigo()));
        assertThat(com2026.aplicadas())
                .as("40 x 1621,00 = 64.840,00: a mesma causa passa a caber. E por isso que o limiar nao "
                        + "pode ser calculado contra o salario minimo de hoje")
                .anyMatch(regra -> "JEC_COMPETENCIA_POTENCIAL".equals(regra.codigo()));
    }

    @Test
    void semDataDeReferenciaNaoEmiteAlertaDeCompetenciaNemConsultaOSalarioMinimo() {
        SalarioMinimoNacionalService servico = mock(SalarioMinimoNacionalService.class);
        NationalRulePackEngine engine = new NationalRulePackEngine(servico);

        var resultado = engine.aplicar(contextoCivil(Map.of("valorCausa", new BigDecimal("1000.00"))));

        assertThat(resultado.aplicadas())
                .as("sem marco do dominio o alerta nao sai: competencia calculada contra o salario "
                        + "errado e pior que competencia nao sinalizada")
                .noneMatch(regra -> "JEC_COMPETENCIA_POTENCIAL".equals(regra.codigo()));
        verify(servico, never()).multiplicar(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    void competenciaDoJuizadoEspecialFederalUsaSessentaSalariosDaDataDeReferencia() {
        SalarioMinimoNacionalService servico = mock(SalarioMinimoNacionalService.class);
        when(servico.multiplicar(any(BigDecimal.class), any(LocalDate.class)))
                .thenAnswer(inv -> inv.<BigDecimal>getArgument(0).multiply(new BigDecimal("1412.00")));
        NationalRulePackEngine engine = new NationalRulePackEngine(servico);

        var resultado = engine.aplicar(new NationalRulePackEngine.ContextoRegra(
                "PROCEDIMENTO COMUM", "Beneficio previdenciario", RamoDireito.PREVIDENCIARIO, null, "TRF5",
                Map.of("valorCausa", new BigDecimal("80000.00"), "dataReferencia", DISTRIBUICAO)));

        assertThat(resultado.aplicadas())
                .as("60 x 1412,00 = 84.720,00, entao causa de 80.000,00 cabe no JEF")
                .anyMatch(regra -> "JEF_COMPETENCIA_POTENCIAL".equals(regra.codigo()));
        verify(servico).multiplicar(eq(new BigDecimal("60")), eq(DISTRIBUICAO));
    }
}
