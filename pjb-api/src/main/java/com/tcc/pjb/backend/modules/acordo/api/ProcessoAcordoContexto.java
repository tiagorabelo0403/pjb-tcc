package com.tcc.pjb.backend.modules.acordo.api;

public record ProcessoAcordoContexto(
        Long processoId,
        String faseProcessual,
        boolean segredoJustica,
        boolean permiteAcordo,
        String classeProcessual,
        Long unidadeJudiciariaId,
        Long magistradoId,
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
        Integer potencialAcordoScore,
        String resumoJanela
) {
}
