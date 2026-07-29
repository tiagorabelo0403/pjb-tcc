package com.tcc.pjb.backend.service.recuperacaojudicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecuperacaoJudicialFalenciaTest {

    private static final LocalDate DATA_PEDIDO_2025 = LocalDate.of(2025, 6, 15);
    private static final BigDecimal SALARIO_MINIMO_2025 = new BigDecimal("1518.00");
    private static final BigDecimal SALARIO_MINIMO_2026 = new BigDecimal("1621.00");

    private final RecuperacaoJudicialReadinessService rj = new RecuperacaoJudicialReadinessService();
    private final PlanoRecuperacaoJudicialService plano = new PlanoRecuperacaoJudicialService();
    private final SalarioMinimoNacionalService salarioMinimoNacionalService = mockSalarioService();
    private final FalenciaDecretacaoService falencia = new FalenciaDecretacaoService(salarioMinimoNacionalService);
    private final QuadroGeralCredoresAssemblerService quadro = new QuadroGeralCredoresAssemblerService();
    private final RecuperacaoExtrajudicialService crj = new RecuperacaoExtrajudicialService();

    private static SalarioMinimoNacionalService mockSalarioService() {
        SalarioMinimoNacionalService service = mock(SalarioMinimoNacionalService.class);
        when(service.multiplicar(any(BigDecimal.class), any(LocalDate.class))).thenReturn(new BigDecimal("60720.00"));
        return service;
    }

    @Test
    void recuperacaoSemPendenciasEmpresaCom3Anos() {
        var input = new RecuperacaoJudicialReadinessService.RecuperacaoJudicialInput(
                "12.345.678/0001-90",
                LocalDate.now().minusYears(3),
                false, null, false, false,
                new BigDecimal("500000.00"), true);
        var result = rj.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.documentosNecessarios()).isNotEmpty();
        assertThat(result.sinalizacao()).contains("validação judicial");
    }

    @Test
    void recuperacaoComPendenciaEmpresaCom1Ano() {
        var input = new RecuperacaoJudicialReadinessService.RecuperacaoJudicialInput(
                "12.345.678/0001-91",
                LocalDate.now().minusMonths(14),
                false, null, false, false,
                new BigDecimal("100000.00"), true);
        var result = rj.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("2 anos"));
    }

    @Test
    void recuperacaoComPendenciaFalenciaAnteriorRecente() {
        var input = new RecuperacaoJudicialReadinessService.RecuperacaoJudicialInput(
                "12.345.678/0001-92",
                LocalDate.now().minusYears(5),
                true, LocalDate.now().minusYears(3),
                false, false,
                new BigDecimal("200000.00"), true);
        var result = rj.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("5 anos"));
    }

    @Test
    void planoMaioriaAtingidaQuandoTodasClassesVotamFavor() {
        var classes = List.of(
                new PlanoRecuperacaoJudicialService.VotoClasse("Trabalhistas", 10, 7, 500000L, 350000L),
                new PlanoRecuperacaoJudicialService.VotoClasse("Quirografários", 20, 12, 1000000L, 650000L));
        var input = new PlanoRecuperacaoJudicialService.PlanoVotacaoInput("RJ001", classes, false);
        var result = plano.avaliarVotacao(input);
        assertThat(result.apuracao()).isEqualTo(PlanoRecuperacaoJudicialService.ResultadoVotacaoAG.MAIORIA_DUPLA_ATINGIDA);
        assertThat(result.classesComMaioria()).hasSize(2);
    }

    @Test
    void planoCramDownQuandoMaisDe50PorcentoClassesAprovam() {
        var classes = List.of(
                new PlanoRecuperacaoJudicialService.VotoClasse("Trabalhistas", 10, 8, 500000L, 420000L),
                new PlanoRecuperacaoJudicialService.VotoClasse("Garantia Real", 5, 3, 800000L, 500000L),
                new PlanoRecuperacaoJudicialService.VotoClasse("Quirografários", 20, 4, 1000000L, 200000L));
        var input = new PlanoRecuperacaoJudicialService.PlanoVotacaoInput("RJ002", classes, false);
        var result = plano.avaliarVotacao(input);
        assertThat(result.apuracao()).isEqualTo(PlanoRecuperacaoJudicialService.ResultadoVotacaoAG.CRAM_DOWN_CONFIGURADO);
        assertThat(result.sinalizacao()).containsIgnoringCase("cram down");
    }

    @Test
    void falenciaSemPendenciaImpontualidadeAcima40SM() {
        var input = new FalenciaDecretacaoService.FalenciaInput(
                "12.000.000/0001-00",
                FalenciaDecretacaoService.CausaFalencia.IMPONTUALIDADE,
                new BigDecimal("100000.00"),
                false, false, null, false, DATA_PEDIDO_2025);
        var result = falencia.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.causaApurada()).isEqualTo(FalenciaDecretacaoService.CausaFalencia.IMPONTUALIDADE);
    }

    @Test
    void falenciaComPendenciaImpontualidadeAbaixo40SM() {
        var input = new FalenciaDecretacaoService.FalenciaInput(
                "12.000.000/0001-01",
                FalenciaDecretacaoService.CausaFalencia.IMPONTUALIDADE,
                new BigDecimal("500.00"),
                false, false, null, false, DATA_PEDIDO_2025);
        var result = falencia.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("40 salários"));
    }

    @Test
    void falenciaComPendenciaStayPeriodRecuperacaoPendente() {
        var input = new FalenciaDecretacaoService.FalenciaInput(
                "12.000.000/0001-02",
                FalenciaDecretacaoService.CausaFalencia.EXECUCAO_FRUSTRADA,
                null, true, false, null, true, DATA_PEDIDO_2025);
        var result = falencia.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("stay"));
    }

    @Test
    void limiteDe40SalariosUsaDataDoPedidoNaoDataAtual() {
        SalarioMinimoNacionalService serviceEspecifico = mock(SalarioMinimoNacionalService.class);
        when(serviceEspecifico.multiplicar(eq(new BigDecimal("40")), eq(DATA_PEDIDO_2025)))
                .thenReturn(new BigDecimal("40").multiply(SALARIO_MINIMO_2025));
        when(serviceEspecifico.multiplicar(eq(new BigDecimal("40")), eq(LocalDate.of(2026, 6, 15))))
                .thenReturn(new BigDecimal("40").multiply(SALARIO_MINIMO_2026));
        FalenciaDecretacaoService falenciaEspecifica = new FalenciaDecretacaoService(serviceEspecifico);

        var inputBaixo2025 = new FalenciaDecretacaoService.FalenciaInput(
                "12.000.000/0001-03",
                FalenciaDecretacaoService.CausaFalencia.IMPONTUALIDADE,
                new BigDecimal("60000.00"),
                false, false, null, false, DATA_PEDIDO_2025);
        var resultBaixo2025 = falenciaEspecifica.avaliar(inputBaixo2025);

        assertThat(resultBaixo2025.pendenciasIdentificadas())
                .as("60000 > 40*1518=60720? Não. Deve entrar como pendência quando SM é de 2025.")
                .anyMatch(i -> i.contains("40 salários"));

        var inputAlto2025 = new FalenciaDecretacaoService.FalenciaInput(
                "12.000.000/0001-04",
                FalenciaDecretacaoService.CausaFalencia.IMPONTUALIDADE,
                new BigDecimal("60800.00"),
                false, false, null, false, DATA_PEDIDO_2025);
        var resultAlto2025 = falenciaEspecifica.avaliar(inputAlto2025);

        assertThat(resultAlto2025.pendenciasIdentificadas())
                .as("60800 > 40*1518=60720? Sim. Deve ser indicativo com SM de 2025.")
                .isEmpty();
    }

    @Test
    void dataPedidoNulaFalhaExplicitamenteEmVezDeCairEmDataAtual() {
        var input = new FalenciaDecretacaoService.FalenciaInput(
                "12.000.000/0001-05",
                FalenciaDecretacaoService.CausaFalencia.IMPONTUALIDADE,
                new BigDecimal("100000.00"),
                false, false, null, false, null);
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> falencia.avaliar(input)))
                .hasMessageContaining("dataPedido");
    }

    @Test
    void quadroGeralOrdenadoPorClasse() {
        var credores = List.of(
                new QuadroGeralCredoresAssemblerService.Credor(
                        "José Silva", "123.456.789-00", new BigDecimal("30000"),
                        QuadroGeralCredoresAssemblerService.ClasseCredor.CLASSE_I_TRABALHISTA, "Verbas rescisórias", true),
                new QuadroGeralCredoresAssemblerService.Credor(
                        "Banco XYZ", "01.234.567/0001-89", new BigDecimal("200000"),
                        QuadroGeralCredoresAssemblerService.ClasseCredor.CLASSE_II_GARANTIA_REAL, "Hipoteca", true),
                new QuadroGeralCredoresAssemblerService.Credor(
                        "Fornecedor ABC", "98.765.432/0001-10", new BigDecimal("50000"),
                        QuadroGeralCredoresAssemblerService.ClasseCredor.CLASSE_III_QUIROGRAFARIO, "Compra mercadorias", true));
        var result = quadro.montar(credores);
        assertThat(result.credoresOrdenados()).hasSize(3);
        assertThat(result.totalClasseI()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.totalGeral()).isEqualByComparingTo(new BigDecimal("280000"));
    }

    @Test
    void crjSemPendenciasHomologacaoViavelCom65PorcentoCredores() {
        var input = new RecuperacaoExtrajudicialService.RecuperacaoExtrajudicialInput(
                "12.000.000/0001-03", 100, 65, true, true);
        var result = crj.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.percentualAcordo()).isEqualTo(65.0);
        assertThat(result.sinalizacao()).contains("validação judicial");
    }

    @Test
    void crjComPendenciaAbaixo60Porcento() {
        var input = new RecuperacaoExtrajudicialService.RecuperacaoExtrajudicialInput(
                "12.000.000/0001-04", 100, 40, true, true);
        var result = crj.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(i -> i.contains("60%"));
    }

    @Test
    void situacaoRecuperacaoAtivaCobre5Estados() {
        assertThat(SituacaoRecuperacaoJudicial.DEFERIMENTO_PROCESSAMENTO.isAtiva()).isTrue();
        assertThat(SituacaoRecuperacaoJudicial.EM_CUMPRIMENTO.isAtiva()).isTrue();
        assertThat(SituacaoRecuperacaoJudicial.ENCERRADA.isAtiva()).isFalse();
        assertThat(SituacaoRecuperacaoJudicial.CONVOLADA_EM_FALENCIA.isAtiva()).isFalse();
    }
}
