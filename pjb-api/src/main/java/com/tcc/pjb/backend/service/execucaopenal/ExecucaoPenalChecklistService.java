package com.tcc.pjb.backend.service.execucaopenal;

import org.springframework.stereotype.Service;

@Service
public class ExecucaoPenalChecklistService {

    public enum RegimePenal { FECHADO, SEMIABERTO, ABERTO }

    public enum TipoCrime {
        COMUM,
        HEDIONDO_SEM_RESULTADO_MORTE,
        HEDIONDO_COM_RESULTADO_MORTE,
        TRAFICO_DROGAS,
        CRIME_CONTRA_CRIANCA_ADOLESCENTE
    }

    public enum TipoReincidencia {
        NAO_REINCIDENTE,
        REINCIDENTE_NAO_ESPECIFICO,
        REINCIDENTE_ESPECIFICO
    }

    public record ExecucaoPenalInput(
            RegimePenal regimeAtual,
            TipoCrime tipoCrime,
            TipoReincidencia reincidencia,
            int penaTotalMeses,
            int penaCumpridaMeses,
            boolean bomComportamentoCarcerario,
            int diasTrabalhadosParaRemicao,
            int horasEstudoParaRemicao
    ) {}

    public record ProgressaoRegimeResult(
            boolean progressaoCabivel,
            RegimePenal proximoRegime,
            int mesesNecessarios,
            int mesesCumpridos,
            int mesesFaltantes,
            double percentualNecessario,
            String fundamentoLegal
    ) {}

    public record RemicaoResult(
            int diasRemidosPorTrabalho,
            int diasRemidosPorEstudo,
            int totalDiasRemidos,
            String fundamentoLegal
    ) {}

    public record ExecucaoPenalResult(
            ProgressaoRegimeResult progressao,
            RemicaoResult remicao,
            int penaTotalEfetivaComRemicaoMeses,
            String observacao
    ) {}

    public ExecucaoPenalResult avaliar(ExecucaoPenalInput input) {
        ProgressaoRegimeResult progressao = calcularProgressao(input);
        RemicaoResult remicao = calcularRemicao(input);

        int mesesRemidos = remicao.totalDiasRemidos() / 30;
        int penaTotalEfetiva = Math.max(0, input.penaTotalMeses() - mesesRemidos);

        String obs = buildObservacao(input, progressao);

        return new ExecucaoPenalResult(progressao, remicao, penaTotalEfetiva, obs);
    }

    private ProgressaoRegimeResult calcularProgressao(ExecucaoPenalInput input) {
        if (input.regimeAtual() == RegimePenal.ABERTO) {
            return new ProgressaoRegimeResult(false, RegimePenal.ABERTO, 0,
                    input.penaCumpridaMeses(), 0, 0,
                    "Já em regime aberto — progressão não se aplica");
        }

        if (!input.bomComportamentoCarcerario()) {
            RegimePenal proximo = input.regimeAtual() == RegimePenal.FECHADO
                    ? RegimePenal.SEMIABERTO : RegimePenal.ABERTO;
            return new ProgressaoRegimeResult(false, proximo,
                    calcularMesesNecessarios(input),
                    input.penaCumpridaMeses(),
                    Math.max(0, calcularMesesNecessarios(input) - input.penaCumpridaMeses()),
                    calcularPercentual(input),
                    "Requisito subjetivo não atendido — bom comportamento carcerário exigido (LEP art. 112)");
        }

        int mesesNecessarios = calcularMesesNecessarios(input);
        boolean prazoAtendido = input.penaCumpridaMeses() >= mesesNecessarios;
        RegimePenal proximo = input.regimeAtual() == RegimePenal.FECHADO
                ? RegimePenal.SEMIABERTO : RegimePenal.ABERTO;

        return new ProgressaoRegimeResult(
                prazoAtendido,
                proximo,
                mesesNecessarios,
                input.penaCumpridaMeses(),
                Math.max(0, mesesNecessarios - input.penaCumpridaMeses()),
                calcularPercentual(input),
                buildFundamentoProgressao(input));
    }

    private int calcularMesesNecessarios(ExecucaoPenalInput input) {
        double percentual = calcularPercentual(input);
        return (int) Math.ceil(input.penaTotalMeses() * percentual);
    }

    private double calcularPercentual(ExecucaoPenalInput input) {
        // LEP art. 112 — redação dada pela Lei 13.964/19 (Pacote Anticrime)
        return switch (input.tipoCrime()) {
            case COMUM -> switch (input.reincidencia()) {
                case NAO_REINCIDENTE -> 0.16;
                case REINCIDENTE_NAO_ESPECIFICO -> 0.20;
                case REINCIDENTE_ESPECIFICO -> 0.20;
            };
            case TRAFICO_DROGAS, HEDIONDO_SEM_RESULTADO_MORTE -> switch (input.reincidencia()) {
                case NAO_REINCIDENTE -> 0.40;
                case REINCIDENTE_NAO_ESPECIFICO, REINCIDENTE_ESPECIFICO -> 0.60;
            };
            case HEDIONDO_COM_RESULTADO_MORTE -> switch (input.reincidencia()) {
                case NAO_REINCIDENTE -> 0.50;
                case REINCIDENTE_NAO_ESPECIFICO, REINCIDENTE_ESPECIFICO -> 0.70;
            };
            case CRIME_CONTRA_CRIANCA_ADOLESCENTE -> switch (input.reincidencia()) {
                case NAO_REINCIDENTE -> 0.50;
                case REINCIDENTE_NAO_ESPECIFICO, REINCIDENTE_ESPECIFICO -> 0.70;
            };
        };
    }

    private String buildFundamentoProgressao(ExecucaoPenalInput input) {
        return switch (input.tipoCrime()) {
            case COMUM -> "LEP art. 112, I/II — crime comum: 16% (não reincidente) ou 20% (reincidente)";
            case TRAFICO_DROGAS -> "LEP art. 112, V/VII — tráfico: 40% (sem reincidência) ou 60% (com reincidência)";
            case HEDIONDO_SEM_RESULTADO_MORTE -> "LEP art. 112, V/VII — hediondo: 40% (sem reincidência) ou 60% (com reincidência)";
            case HEDIONDO_COM_RESULTADO_MORTE -> "LEP art. 112, VI/VIII — hediondo com morte: 50% (sem reincidência) ou 70% (com reincidência)";
            case CRIME_CONTRA_CRIANCA_ADOLESCENTE -> "LEP art. 112, VII/VIII — crime contra criança/adolescente: 50% ou 70%";
        };
    }

    private RemicaoResult calcularRemicao(ExecucaoPenalInput input) {
        // LEP art. 126, §1°, I: 1 dia de pena para cada 3 dias trabalhados
        int diasRemidosPorTrabalho = input.diasTrabalhadosParaRemicao() / 3;

        // LEP art. 126, §1°, II: 1 dia de pena para cada 12 horas de estudo
        int diasRemidosPorEstudo = input.horasEstudoParaRemicao() / 12;

        return new RemicaoResult(
                diasRemidosPorTrabalho,
                diasRemidosPorEstudo,
                diasRemidosPorTrabalho + diasRemidosPorEstudo,
                "LEP art. 126, §1°: remição por trabalho (1 dia/3 trabalhados) e estudo (1 dia/12h)");
    }

    private String buildObservacao(ExecucaoPenalInput input, ProgressaoRegimeResult progressao) {
        StringBuilder obs = new StringBuilder();

        if (progressao.progressaoCabivel()) {
            obs.append("Requisitos objetivo e subjetivo atendidos — progressão ao regime ")
               .append(progressao.proximoRegime()).append(" cabível. ");
        } else if (!input.bomComportamentoCarcerario()) {
            obs.append("Requisito subjetivo (bom comportamento) não atendido. Necessário atestado do diretor do estabelecimento (LEP art. 112, §1°). ");
        } else {
            obs.append(String.format("Faltam %d meses para atingir %.0f%% da pena. ",
                    progressao.mesesFaltantes(), progressao.percentualNecessario() * 100));
        }

        obs.append("Remição não se perde por falta grave, salvo revogação parcial (LEP art. 127). ");
        obs.append("Livramento condicional: CP art. 83 — 1/3 (não reincidente), 1/2 (reincidente), 2/3 (hediondo sem reincidência específica).");

        return obs.toString();
    }
}
