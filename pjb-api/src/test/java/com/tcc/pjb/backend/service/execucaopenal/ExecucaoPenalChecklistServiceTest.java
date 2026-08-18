package com.tcc.pjb.backend.service.execucaopenal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.execucaopenal.ExecucaoPenalChecklistService.ExecucaoPenalInput;
import com.tcc.pjb.backend.service.execucaopenal.ExecucaoPenalChecklistService.ExecucaoPenalResult;
import com.tcc.pjb.backend.service.execucaopenal.ExecucaoPenalChecklistService.RegimePenal;
import com.tcc.pjb.backend.service.execucaopenal.ExecucaoPenalChecklistService.TipoCrime;
import com.tcc.pjb.backend.service.execucaopenal.ExecucaoPenalChecklistService.TipoReincidencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExecucaoPenalChecklistServiceTest {

    private final ExecucaoPenalChecklistService service = new ExecucaoPenalChecklistService();

    @Test
    void deveAutorizarProgressaoFechadoParaSemiabertoCrimeComum16Porcento() {
        ExecucaoPenalInput input = new ExecucaoPenalInput(
                RegimePenal.FECHADO, TipoCrime.COMUM, TipoReincidencia.NAO_REINCIDENTE,
                120, 20, true, 0, 0);

        ExecucaoPenalResult result = service.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isTrue();
        assertThat(result.progressao().proximoRegime()).isEqualTo(RegimePenal.SEMIABERTO);
    }

    @Test
    void deveNegarProgressaoQuandoBomComportamentoAusente() {
        ExecucaoPenalInput input = new ExecucaoPenalInput(
                RegimePenal.FECHADO, TipoCrime.COMUM, TipoReincidencia.NAO_REINCIDENTE,
                120, 30, false, 0, 0);

        ExecucaoPenalResult result = service.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isFalse();
        assertThat(result.progressao().fundamentoLegal()).contains("bom comportamento");
    }

    @Test
    void deveNegarProgressaoQuandoPrazoInsuficiente() {
        ExecucaoPenalInput input = new ExecucaoPenalInput(
                RegimePenal.FECHADO, TipoCrime.COMUM, TipoReincidencia.NAO_REINCIDENTE,
                120, 10, true, 0, 0);

        ExecucaoPenalResult result = service.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isFalse();
        assertThat(result.progressao().mesesFaltantes()).isGreaterThan(0);
    }

    @ParameterizedTest(name = "crime={0} reincidencia={1} → percentual={2}")
    @CsvSource({
            "COMUM,                        NAO_REINCIDENTE,           0.16",
            "COMUM,                        REINCIDENTE_NAO_ESPECIFICO, 0.20",
            "TRAFICO_DROGAS,               NAO_REINCIDENTE,           0.40",
            "TRAFICO_DROGAS,               REINCIDENTE_ESPECIFICO,    0.60",
            "HEDIONDO_COM_RESULTADO_MORTE,  NAO_REINCIDENTE,           0.50",
            "HEDIONDO_COM_RESULTADO_MORTE,  REINCIDENTE_ESPECIFICO,    0.70"
    })
    void deveAplicarPercentualCorretoConformeLeP(TipoCrime crime, TipoReincidencia reincidencia,
                                                 double percentualEsperado) {
        int penaMeses = 100;
        int mesesNecessarios = (int) Math.ceil(penaMeses * percentualEsperado);
        ExecucaoPenalInput input = new ExecucaoPenalInput(
                RegimePenal.FECHADO, crime, reincidencia,
                penaMeses, mesesNecessarios, true, 0, 0);

        ExecucaoPenalResult result = service.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isTrue();
    }

    @Test
    void deveCalcularRemicaoPorTrabalho() {
        ExecucaoPenalInput input = new ExecucaoPenalInput(
                RegimePenal.SEMIABERTO, TipoCrime.COMUM, TipoReincidencia.NAO_REINCIDENTE,
                60, 12, true, 90, 0);

        ExecucaoPenalResult result = service.avaliar(input);

        assertThat(result.remicao().diasRemidosPorTrabalho()).isEqualTo(30);
        assertThat(result.remicao().fundamentoLegal()).contains("126");
    }

    @Test
    void deveCalcularRemicaoPorEstudo() {
        ExecucaoPenalInput input = new ExecucaoPenalInput(
                RegimePenal.SEMIABERTO, TipoCrime.COMUM, TipoReincidencia.NAO_REINCIDENTE,
                60, 12, true, 0, 120);

        ExecucaoPenalResult result = service.avaliar(input);

        assertThat(result.remicao().diasRemidosPorEstudo()).isEqualTo(10);
    }

    @Test
    void deveRetornarProgressaoNaoAplicavelParaRegimeAberto() {
        ExecucaoPenalInput input = new ExecucaoPenalInput(
                RegimePenal.ABERTO, TipoCrime.COMUM, TipoReincidencia.NAO_REINCIDENTE,
                60, 50, true, 0, 0);

        ExecucaoPenalResult result = service.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isFalse();
        assertThat(result.progressao().proximoRegime()).isEqualTo(RegimePenal.ABERTO);
    }

    @Test
    void deveDescontarRemicaoDaPenaTotal() {
        ExecucaoPenalInput input = new ExecucaoPenalInput(
                RegimePenal.SEMIABERTO, TipoCrime.COMUM, TipoReincidencia.NAO_REINCIDENTE,
                24, 5, true, 60, 0);

        ExecucaoPenalResult result = service.avaliar(input);

        int diasRemidos = result.remicao().totalDiasRemidos();
        int mesesRemidos = diasRemidos / 30;
        assertThat(result.penaTotalEfetivaComRemicaoMeses()).isEqualTo(Math.max(0, 24 - mesesRemidos));
    }
}
