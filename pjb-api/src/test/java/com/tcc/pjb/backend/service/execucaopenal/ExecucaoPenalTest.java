package com.tcc.pjb.backend.service.execucaopenal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.execucaopenal.PenaPrivativaCalculatorService.ConcursoTipo;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecucaoPenalTest {

    private final PenaPrivativaCalculatorService calculator = new PenaPrivativaCalculatorService();
    private final ProgressaoRegimeService progressao = new ProgressaoRegimeService();
    private final DetracaoRemicaoService detracao = new DetracaoRemicaoService();
    private final LivramentoCondicionalService livramento = new LivramentoCondicionalService();
    private final ExtincaoPunibilidadeService extincao = new ExtincaoPunibilidadeService();
    private final MedidaSegurancaService medida = new MedidaSegurancaService();

    @Test
    void concursoMaterialDeveSomarPenas() {
        var input = new PenaPrivativaCalculatorService.PenaInput(List.of(60, 48, 36), ConcursoTipo.MATERIAL, false);
        var result = calculator.unificar(input);
        assertThat(result.totalMeses()).isEqualTo(144);
        assertThat(result.fundamentacao()).contains("material");
    }

    @Test
    void concursoFormalProprioAplicaUmSextoSobreMaiorPena() {
        var input = new PenaPrivativaCalculatorService.PenaInput(List.of(120, 60), ConcursoTipo.FORMAL_PROPRIO, false);
        var result = calculator.unificar(input);
        assertThat(result.totalMeses()).isEqualTo(140);
    }

    @Test
    void crimeContinuadoAplicaDoisTercosSobreMaiorPena() {
        var input = new PenaPrivativaCalculatorService.PenaInput(List.of(60, 48), ConcursoTipo.CONTINUADO, false);
        var result = calculator.unificar(input);
        assertThat(result.totalMeses()).isGreaterThan(60);
        assertThat(result.fundamentacao()).contains("continuado");
    }

    @Test
    void progressaoNegadaPorFracaoInsuficiente() {
        var input = new ProgressaoRegimeService.ProgressaoInput(
                "P001", 120, 10, RegimePrisionalTipo.FECHADO,
                false, false, false, false, false,
                true, LocalDate.now().minusMonths(10));
        var result = progressao.avaliar(input);
        assertThat(result.progressaoPossivel()).isFalse();
        assertThat(result.mesesNecessarios()).isGreaterThan(0);
        assertThat(result.impeditivos()).isNotEmpty();
    }

    @Test
    void progressaoAprovadaParaCrimeComumNaoReincidente() {
        var inicio = LocalDate.now().minusMonths(25);
        var input = new ProgressaoRegimeService.ProgressaoInput(
                "P002", 120, 25, RegimePrisionalTipo.FECHADO,
                false, false, false, false, true,
                true, inicio);
        var result = progressao.avaliar(input);
        assertThat(result.progressaoPossivel()).isTrue();
        assertThat(result.proximoRegime()).isEqualTo(RegimePrisionalTipo.SEMIABERTO);
        assertThat(result.fracao()).isEqualTo(FracaoProgressaoRegime.VINTE_PORCENTO);
    }

    @Test
    void fracaoHediondoPrimarioSemMorte40porcento() {
        var fracao = FracaoProgressaoRegime.resolver(true, false, false, false, false);
        assertThat(fracao).isEqualTo(FracaoProgressaoRegime.QUARENTA_PORCENTO);
    }

    @Test
    void fracaoHediondoComMorteReincidente70porcento() {
        var fracao = FracaoProgressaoRegime.resolver(true, true, true, true, true);
        assertThat(fracao).isEqualTo(FracaoProgressaoRegime.SETENTA_PORCENTO);
    }

    @Test
    void detracaoDeveSubtrairPrisaoProvisoria() {
        var input = new DetracaoRemicaoService.DetracaoRemicaoInput(
                1800, 180, 0, 0, 0, 0, false);
        var result = detracao.calcular(input);
        assertThat(result.diasDetracaoTotal()).isEqualTo(180);
        assertThat(result.penaSaldo()).isEqualTo(1620);
    }

    @Test
    void remicaoTrabalhoReducesPena() {
        var input = new DetracaoRemicaoService.DetracaoRemicaoInput(
                900, 0, 0, 90, 0, 0, true);
        var result = detracao.calcular(input);
        assertThat(result.diasRemicaoTrabalho()).isEqualTo(30);
        assertThat(result.penaSaldo()).isEqualTo(870);
    }

    @Test
    void livramentoPrimarioBoaCondutaUmTercoDaPena() {
        var input = new LivramentoCondicionalService.LivramentoInput(
                60, 22, false, false, true, true, false, false);
        var result = livramento.avaliar(input);
        assertThat(result.cabivel()).isTrue();
        assertThat(result.mesesMinimos()).isEqualTo(20);
        assertThat(result.requisitosAtendidos()).isTrue();
    }

    @Test
    void livramentoHediondoDoisTercos() {
        var input = new LivramentoCondicionalService.LivramentoInput(
                60, 35, true, false, true, true, false, false);
        var result = livramento.avaliar(input);
        assertThat(result.mesesMinimos()).isEqualTo(40);
        assertThat(result.requisitosAtendidos()).isFalse();
    }

    @Test
    void extincaoPorMorteConfirmada() {
        var input = new ExtincaoPunibilidadeService.ExtincaoInput(
                "P003", 4, 0, LocalDate.now().minusYears(2),
                null, null, null,
                true, false, false, false, false);
        var result = extincao.verificar(input);
        assertThat(result.extinta()).isTrue();
        assertThat(result.causa()).isPresent();
        assertThat(result.causa().get()).isInstanceOf(ExtincaoPunibilidadeCausa.MorteDoAgente.class);
    }

    @Test
    void extincaoPorPrescricaoPunitiva() {
        var input = new ExtincaoPunibilidadeService.ExtincaoInput(
                "P004", 1, 0,
                LocalDate.now().minusYears(3),
                null, null, null,
                false, false, false, false, false);
        var result = extincao.verificar(input);
        assertThat(result.extinta()).isTrue();
        assertThat(result.causa().get()).isInstanceOf(ExtincaoPunibilidadeCausa.Prescricao.class);
    }

    @Test
    void medidaSegurancaInternacaoParaPenaMaximaAcimaDeUmAno() {
        var input = new MedidaSegurancaService.MedidaSegurancaInput(
                "P005", 4, 0, true, false, 0, false);
        var result = medida.avaliar(input);
        assertThat(result.modalidade()).isEqualTo(MedidaSegurancaService.ModalidadeMedidaSeguranca.INTERNACAO);
        assertThat(result.prazoMinimoAnos()).isEqualTo(1);
    }

    @Test
    void medidaSegurancaAmbulatorial() {
        var input = new MedidaSegurancaService.MedidaSegurancaInput(
                "P006", 0, 0, true, false, 2, false);
        var result = medida.avaliar(input);
        assertThat(result.modalidade()).isEqualTo(MedidaSegurancaService.ModalidadeMedidaSeguranca.TRATAMENTO_AMBULATORIAL);
    }

    @Test
    void desinternacaoProgressivaCabivelAposPrazoMinimo() {
        var input = new MedidaSegurancaService.MedidaSegurancaInput(
                "P007", 4, 0, true, false, 2, false);
        var result = medida.avaliar(input);
        assertThat(result.desinternacaoProgressivaCabivel()).isTrue();
    }

    @Test
    void regimeSemiAbertoProximoEAberto() {
        assertThat(RegimePrisionalTipo.FECHADO.proximo()).isEqualTo(RegimePrisionalTipo.SEMIABERTO);
        assertThat(RegimePrisionalTipo.SEMIABERTO.proximo()).isEqualTo(RegimePrisionalTipo.ABERTO);
        assertThat(RegimePrisionalTipo.FECHADO.isMaisGraveQue(RegimePrisionalTipo.SEMIABERTO)).isTrue();
    }
}
