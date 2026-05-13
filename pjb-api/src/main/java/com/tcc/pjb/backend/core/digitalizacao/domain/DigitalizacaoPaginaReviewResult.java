package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoPaginaReviewResult(Long paginaId,
                                              Long jobId,
                                              boolean jobConcluido,
                                              long paginasPendentes) {
}
