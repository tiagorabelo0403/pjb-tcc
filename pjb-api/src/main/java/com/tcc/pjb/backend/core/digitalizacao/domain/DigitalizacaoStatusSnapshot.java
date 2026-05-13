package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoStatusSnapshot(Long jobId, String status, boolean revisaoRequerida) {}
