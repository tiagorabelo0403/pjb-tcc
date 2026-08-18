package com.tcc.pjb.backend.modules.acordo.domain;

import java.util.Objects;

public class AcordoProcessualWindowPolicy {

    public AcordoProcessualWindowDecision avaliar(AcordoProcessualWindowInput input) {
        Objects.requireNonNull(input, "input");
        if (input.determinacaoJudicial() || input.cejuscReferenciado()) {
            return permitido(
                    "Abertura autorizada por determinacao judicial ou CEJUSC.",
                    input.cejuscReferenciado() ? AcordoTipoSala.CEJUSC : AcordoTipoSala.PROCESSUAL_CONTROLADA,
                    input,
                    input.cejuscReferenciado(),
                    exigeAdvogado(input),
                    true,
                    AcordoMomentoProcessual.DETERMINACAO_JUDICIAL_CEJUSC
            );
        }
        if (input.mutiraoConciliacao()) {
            return permitido(
                    "Abertura autorizada por mutirao de conciliacao.",
                    AcordoTipoSala.CEJUSC,
                    input,
                    true,
                    exigeAdvogado(input),
                    false,
                    AcordoMomentoProcessual.MUTIRAO_CONCILIACAO
            );
        }
        if (input.antesContestacao()) {
            return permitido(
                    "Abertura permitida antes da contestacao.",
                    AcordoTipoSala.CONCILIACAO,
                    input,
                    false,
                    exigeAdvogado(input),
                    false,
                    AcordoMomentoProcessual.ANTES_CONTESTACAO
            );
        }
        if (input.antesAudienciaConciliacaoMediacao()) {
            return permitido(
                    "Abertura permitida antes da audiencia de conciliacao ou mediacao.",
                    AcordoTipoSala.CONCILIACAO,
                    input,
                    true,
                    exigeAdvogado(input),
                    false,
                    AcordoMomentoProcessual.ANTES_AUDIENCIA_CONCILIACAO_MEDIACAO
            );
        }
        if (input.aposContestacao() && input.propostaFormalExistente()) {
            return permitido(
                    "Abertura permitida apos contestacao com proposta formal registrada.",
                    AcordoTipoSala.PROCESSUAL_CONTROLADA,
                    input,
                    false,
                    true,
                    false,
                    AcordoMomentoProcessual.APOS_CONTESTACAO_COM_PROPOSTA
            );
        }
        if (input.aposContestacao()) {
            return AcordoProcessualWindowDecision.negado("Apos contestacao, a abertura exige proposta formal.");
        }
        if (input.aposPericiaOuLaudo()) {
            return permitido(
                    "Abertura permitida apos pericia ou laudo.",
                    AcordoTipoSala.MEDIACAO,
                    input,
                    true,
                    true,
                    false,
                    AcordoMomentoProcessual.APOS_PERICIA_LAUDO
            );
        }
        if (input.antesSentenca()) {
            return permitido(
                    "Abertura permitida antes da sentenca.",
                    AcordoTipoSala.PROCESSUAL_CONTROLADA,
                    input,
                    false,
                    true,
                    false,
                    AcordoMomentoProcessual.ANTES_SENTENCA
            );
        }
        if (input.faseRecursal()) {
            return permitido(
                    "Abertura permitida em fase recursal.",
                    AcordoTipoSala.RECURSAL,
                    input,
                    false,
                    true,
                    false,
                    AcordoMomentoProcessual.RECURSAL
            );
        }
        if (input.cumprimentoSentencaOuExecucao()) {
            return permitido(
                    "Abertura permitida em cumprimento de sentenca ou execucao.",
                    AcordoTipoSala.EXECUCAO,
                    input,
                    false,
                    true,
                    false,
                    AcordoMomentoProcessual.CUMPRIMENTO_SENTENCA_EXECUCAO
            );
        }
        if (input.requerimentoParte()) {
            return permitido(
                    "Abertura permitida por requerimento de parte.",
                    AcordoTipoSala.PROCESSUAL_CONTROLADA,
                    input,
                    false,
                    exigeAdvogado(input),
                    false,
                    AcordoMomentoProcessual.REQUERIMENTO_PARTE
            );
        }
        return AcordoProcessualWindowDecision.negado("Momento processual fora da janela controlada de acordo.");
    }

    private AcordoProcessualWindowDecision permitido(String motivo,
                                                     AcordoTipoSala tipoSala,
                                                     AcordoProcessualWindowInput input,
                                                     boolean exigeConciliadorMediador,
                                                     boolean exigeAdvogado,
                                                     boolean exigeDeterminacaoJudicial,
                                                     AcordoMomentoProcessual momento) {
        return new AcordoProcessualWindowDecision(
                true,
                motivo,
                tipoSala,
                input.segredoJustica(),
                exigeConciliadorMediador,
                exigeAdvogado,
                exigeDeterminacaoJudicial,
                momento
        );
    }

    private boolean exigeAdvogado(AcordoProcessualWindowInput input) {
        return input.parteSemAdvogado() || input.faseRecursal() || input.cumprimentoSentencaOuExecucao();
    }
}
