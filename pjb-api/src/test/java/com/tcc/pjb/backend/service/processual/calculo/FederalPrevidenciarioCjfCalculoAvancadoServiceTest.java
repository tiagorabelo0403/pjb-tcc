package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.FederalPrevidenciarioCjfCalculoAvancadoRequest;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FederalPrevidenciarioCjfCalculoAvancadoServiceTest {

    private static final BigDecimal SALARIO_MINIMO = new BigDecimal("1518.00");
    private static final LocalDate TRANSITO = LocalDate.of(2025, 11, 18);

    private final SalarioMinimoNacionalService salarioMinimoNacionalService = salarioMinimoNoTransito();

    private final FederalPrevidenciarioCjfCalculoAvancadoService service =
            new FederalPrevidenciarioCjfCalculoAvancadoService(
                    new CalculoJudicialAssistenciaService(
                            new CalculoJudicialProfileResolverService(),
                            new CalculoJudicialFrontendContractService(
                                    new CalculoJudicialTabelaOficialService(),
                                    TestEconomicReferenceSupport.economicReferenceService()),
                            TestEconomicReferenceSupport.tetoRpvNacionalService()),
                    TestEconomicReferenceSupport.tetoRpvNacionalService(),
                    salarioMinimoNacionalService);

    private static SalarioMinimoNacionalService salarioMinimoNoTransito() {
        SalarioMinimoNacionalService mockado = mock(SalarioMinimoNacionalService.class);
        when(mockado.valorEm(TRANSITO)).thenReturn(SALARIO_MINIMO);
        return mockado;
    }

    private static FederalPrevidenciarioCjfCalculoAvancadoRequest pedidoCom(BigDecimal rendaMensal) {
        return pedidoCom(rendaMensal, SALARIO_MINIMO, null);
    }

    private static FederalPrevidenciarioCjfCalculoAvancadoRequest pedidoCom(BigDecimal rendaMensal, BigDecimal salarioMinimo, LocalDate transito) {
        return new FederalPrevidenciarioCjfCalculoAvancadoRequest(
                "Atrasados", "0000001-11.2026.4.05.8100", "TRF5", "CJF", "Aposentadoria por idade",
                CalculoJudicialSolicitantePerfil.ADVOGADO, "Advogada", "OAB/CE 1", rendaMensal,
                LocalDate.of(2024, 1, 1), null, null, null, null, LocalDate.of(2026, 3, 10),
                Boolean.FALSE, Boolean.FALSE, null, null, List.of(), null, null, null,
                salarioMinimo, null, null, null, null, transito);
    }

    @Test
    void semTetoInformadoAClassificacaoUsaOsSessentaSalariosDaUniao() {
        var relatorio = service.calcular(pedidoCom(new BigDecimal("1000.00")), CalculoJudicialSolicitantePerfil.ADVOGADO);

        assertThat(relatorio.totalGeral())
                .as("total precisa caber no teto federal de 60 salarios minimos para o caso valer como prova")
                .isLessThanOrEqualTo(SALARIO_MINIMO.multiply(new BigDecimal("60")));
        assertThat(relatorio.alertas())
                .as("classificacao projetada com teto da Uniao")
                .contains("Classificação projetada do pagamento: RPV.");
    }

    @Test
    void valorAcimaDoTetoFederalProjetaPrecatorio() {
        var relatorio = service.calcular(pedidoCom(new BigDecimal("90000.00")), CalculoJudicialSolicitantePerfil.ADVOGADO);

        assertThat(relatorio.totalGeral()).isGreaterThan(SALARIO_MINIMO.multiply(new BigDecimal("60")));
        assertThat(relatorio.alertas()).contains("Classificação projetada do pagamento: PRECATORIO.");
    }

    @Test
    void semSalarioMinimoInformadoOTetoUsaOSalarioDoTransitoEmJulgado() {
        var relatorio = service.calcular(pedidoCom(new BigDecimal("1000.00"), null, TRANSITO), CalculoJudicialSolicitantePerfil.ADVOGADO);

        verify(salarioMinimoNacionalService).valorEm(TRANSITO);
        assertThat(relatorio.alertas()).contains("Classificação projetada do pagamento: RPV.");
    }

    @Test
    void semSalarioMinimoESemTransitoNaoClassificaOPagamento() {
        var relatorio = service.calcular(pedidoCom(new BigDecimal("1000.00"), null, null), CalculoJudicialSolicitantePerfil.ADVOGADO);

        assertThat(relatorio.alertas()).noneMatch(alerta -> alerta.startsWith("Classificação projetada do pagamento"));
    }

    @Test
    void comTransitoInformadoOSalarioDoTransitoVenceOInformadoEAvisaADivergencia() {
        var relatorio = service.calcular(pedidoCom(new BigDecimal("1000.00"), new BigDecimal("1621.00"), TRANSITO), CalculoJudicialSolicitantePerfil.ADVOGADO);

        verify(salarioMinimoNacionalService).valorEm(TRANSITO);
        assertThat(relatorio.alertas())
                .contains("Salário mínimo informado (1621.00) difere do vigente no trânsito em julgado (1518.00); o teto de RPV usa o do trânsito.");
    }
}
