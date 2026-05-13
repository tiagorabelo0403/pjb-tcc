package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoJobView(Long jobId,
                                   String status,
                                   int totalPaginas,
                                   int paginasProcessadas,
                                   boolean revisaoRequerida) {
}
