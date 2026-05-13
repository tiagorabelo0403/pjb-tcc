package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoReviewCommandResult(Long paginaId,
                                               boolean revisado,
                                               String tipoPeca) {
}
