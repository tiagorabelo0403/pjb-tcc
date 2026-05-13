package com.tcc.pjb.backend.core.eleitoral.domain;

public record FeitoEleitoralPendenteDiplomacaoView(Long processoId,
                                                   String tipoFeito,
                                                   String numeroCandidato,
                                                   Integer anoEleitoral) {
}
