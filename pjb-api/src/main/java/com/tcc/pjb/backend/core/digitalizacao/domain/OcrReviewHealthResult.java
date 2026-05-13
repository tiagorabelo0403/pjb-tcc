package com.tcc.pjb.backend.core.digitalizacao.domain;
public record OcrReviewHealthResult(Long jobId, boolean reviewRequired, boolean healthy, int paginasProcessadas) {}
