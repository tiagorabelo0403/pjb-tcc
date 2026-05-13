package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.time.Instant;

public record DigitalizacaoReviewQueueEntry(
        Long jobId,
        String status,
        int paginasProcessadas,
        boolean revisaoRequerida,
        Long processoId,
        String numeroProcessoOrigem,
        int totalPaginas,
        Instant createdAt
) {
    public DigitalizacaoReviewQueueEntry(Long jobId,
                                         Long processoId,
                                         String numeroProcessoOrigem,
                                         Integer paginasProcessadas,
                                         Integer totalPaginas) {
        this(
                jobId,
                "REVISAO_HUMANA",
                paginasProcessadas == null ? 0 : paginasProcessadas,
                true,
                processoId,
                numeroProcessoOrigem,
                totalPaginas == null ? 0 : totalPaginas,
                null);
    }

    public DigitalizacaoReviewQueueEntry(Long jobId,
                                         String status,
                                         Integer paginasProcessadas,
                                         boolean revisaoRequerida) {
        this(
                jobId,
                status,
                paginasProcessadas == null ? 0 : paginasProcessadas,
                revisaoRequerida,
                null,
                null,
                0,
                null);
    }

    public DigitalizacaoReviewQueueEntry(Long jobId,
                                         Integer paginasProcessadas,
                                         Instant createdAt) {
        this(
                jobId,
                "REVISAO_HUMANA",
                paginasProcessadas == null ? 0 : paginasProcessadas,
                true,
                null,
                null,
                0,
                createdAt);
    }
}
