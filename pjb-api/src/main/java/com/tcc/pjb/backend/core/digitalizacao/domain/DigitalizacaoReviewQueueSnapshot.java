package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.util.List;

public record DigitalizacaoReviewQueueSnapshot(
        List<DigitalizacaoReviewQueueEntry> jobs,
        int total
) {
    public List<Long> jobIds() {
        return jobs.stream().map(DigitalizacaoReviewQueueEntry::jobId).toList();
    }

    public boolean hasJobs() {
        return !jobs.isEmpty();
    }
}
