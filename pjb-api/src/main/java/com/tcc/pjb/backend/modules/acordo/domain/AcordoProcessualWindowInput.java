package com.tcc.pjb.backend.modules.acordo.domain;

public record AcordoProcessualWindowInput(
        String faseProcessual,
        boolean segredoJustica,
        boolean antesContestacao,
        boolean antesAudienciaConciliacaoMediacao,
        boolean aposContestacao,
        boolean propostaFormalExistente,
        boolean aposPericiaOuLaudo,
        boolean antesSentenca,
        boolean faseRecursal,
        boolean cumprimentoSentencaOuExecucao,
        boolean mutiraoConciliacao,
        boolean requerimentoParte,
        boolean determinacaoJudicial,
        boolean cejuscReferenciado,
        boolean parteSemAdvogado,
        Integer potencialAcordoScore
) {
}
