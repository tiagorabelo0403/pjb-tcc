package com.tcc.pjb.backend.core.digitalizacao.domain;

public record OcrExecutionHealthSnapshot(Long jobId, String status, int totalPaginas, int paginasProcessadas, boolean revisaoRequerida) {}
