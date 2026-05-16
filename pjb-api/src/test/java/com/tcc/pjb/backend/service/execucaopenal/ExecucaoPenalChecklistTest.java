package com.tcc.pjb.backend.service.execucaopenal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecucaoPenalChecklistTest {

    private final ExecucaoPenalChecklistService svc = new ExecucaoPenalChecklistService();

    // --- Progressão: crime comum ---

    @Test
    void progressao_crimeComum_naoReincidente_16porcento() {
        // pena 60 meses → 16% = 9,6 → ceil = 10 meses
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 10, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isTrue();
        assertThat(result.progressao().proximoRegime())
                .isEqualTo(ExecucaoPenalChecklistService.RegimePenal.SEMIABERTO);
        assertThat(result.progressao().percentualNecessario()).isEqualTo(0.16);
        assertThat(result.progressao().mesesNecessarios()).isEqualTo(10);
        assertThat(result.progressao().fundamentoLegal()).contains("16%");
    }

    @Test
    void progressao_crimeComum_reincidente_20porcento_naoAtingido() {
        // pena 60 meses → 20% = 12 meses; cumprido 8 → faltam 4
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.REINCIDENTE_NAO_ESPECIFICO,
                60, 8, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isFalse();
        assertThat(result.progressao().mesesFaltantes()).isEqualTo(4);
        assertThat(result.progressao().percentualNecessario()).isEqualTo(0.20);
    }

    // --- Progressão: crime hediondo ---

    @Test
    void progressao_hediondoSemMorte_naoReincidente_40porcento() {
        // pena 120 meses → 40% = 48 meses; cumprido 48
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.HEDIONDO_SEM_RESULTADO_MORTE,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                120, 48, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isTrue();
        assertThat(result.progressao().percentualNecessario()).isEqualTo(0.40);
        assertThat(result.progressao().fundamentoLegal()).contains("40%");
    }

    @Test
    void progressao_hediondoSemMorte_reincidente_60porcento() {
        // pena 120 meses → 60% = 72 meses; cumprido 70 → falta 2
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.HEDIONDO_SEM_RESULTADO_MORTE,
                ExecucaoPenalChecklistService.TipoReincidencia.REINCIDENTE_ESPECIFICO,
                120, 70, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isFalse();
        assertThat(result.progressao().percentualNecessario()).isEqualTo(0.60);
        assertThat(result.progressao().mesesFaltantes()).isEqualTo(2);
    }

    @Test
    void progressao_hediondoComMorte_naoReincidente_50porcento() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.HEDIONDO_COM_RESULTADO_MORTE,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                240, 120, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isTrue();
        assertThat(result.progressao().percentualNecessario()).isEqualTo(0.50);
    }

    @Test
    void progressao_hediondoComMorte_reincidente_70porcento() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.HEDIONDO_COM_RESULTADO_MORTE,
                ExecucaoPenalChecklistService.TipoReincidencia.REINCIDENTE_ESPECIFICO,
                240, 100, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isFalse();
        assertThat(result.progressao().percentualNecessario()).isEqualTo(0.70);
    }

    @Test
    void progressao_trafico_naoReincidente_40porcento() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.TRAFICO_DROGAS,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 24, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isTrue();
        assertThat(result.progressao().percentualNecessario()).isEqualTo(0.40);
    }

    @Test
    void progressao_crimeCriancaAdolescente_naoReincidente_50porcento() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.CRIME_CONTRA_CRIANCA_ADOLESCENTE,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                120, 60, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isTrue();
        assertThat(result.progressao().percentualNecessario()).isEqualTo(0.50);
    }

    // --- Requisito subjetivo ---

    @Test
    void progressao_semBomComportamento_naoPermitida() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.FECHADO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 15, false, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isFalse();
        assertThat(result.progressao().fundamentoLegal()).contains("bom comportamento");
    }

    @Test
    void progressao_regimeAberto_naoSeAplica() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.ABERTO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 30, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.progressao().progressaoCabivel()).isFalse();
        assertThat(result.progressao().fundamentoLegal()).contains("regime aberto");
    }

    // --- Remição ---

    @Test
    void remicao_porTrabalho_1DiaPara3Trabalhados() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.SEMIABERTO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 10, true, 90, 0);

        var result = svc.avaliar(input);

        assertThat(result.remicao().diasRemidosPorTrabalho()).isEqualTo(30);
        assertThat(result.remicao().diasRemidosPorEstudo()).isEqualTo(0);
        assertThat(result.remicao().totalDiasRemidos()).isEqualTo(30);
        assertThat(result.remicao().fundamentoLegal()).contains("LEP art. 126");
    }

    @Test
    void remicao_porEstudo_1DiaPara12Horas() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.SEMIABERTO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 10, true, 0, 120);

        var result = svc.avaliar(input);

        assertThat(result.remicao().diasRemidosPorEstudo()).isEqualTo(10);
        assertThat(result.remicao().diasRemidosPorTrabalho()).isEqualTo(0);
        assertThat(result.remicao().totalDiasRemidos()).isEqualTo(10);
    }

    @Test
    void remicao_combinada_trabalhoEEstudo() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.SEMIABERTO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 10, true, 60, 60);

        var result = svc.avaliar(input);

        assertThat(result.remicao().diasRemidosPorTrabalho()).isEqualTo(20);
        assertThat(result.remicao().diasRemidosPorEstudo()).isEqualTo(5);
        assertThat(result.remicao().totalDiasRemidos()).isEqualTo(25);
    }

    @Test
    void penaTotalEfetiva_reduzidaPelaRemicao() {
        // pena 60 meses; 90 dias trabalhados → 30 dias remidos = 1 mês
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.SEMIABERTO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 10, true, 90, 0);

        var result = svc.avaliar(input);

        assertThat(result.penaTotalEfetivaComRemicaoMeses()).isEqualTo(59);
    }

    @Test
    void observacaoMencionaLivramentoCondicional() {
        var input = new ExecucaoPenalChecklistService.ExecucaoPenalInput(
                ExecucaoPenalChecklistService.RegimePenal.SEMIABERTO,
                ExecucaoPenalChecklistService.TipoCrime.COMUM,
                ExecucaoPenalChecklistService.TipoReincidencia.NAO_REINCIDENTE,
                60, 30, true, 0, 0);

        var result = svc.avaliar(input);

        assertThat(result.observacao()).contains("CP art. 83");
    }
}
