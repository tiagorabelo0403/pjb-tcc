package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoPageWindowView(
        Long jobId,
        int fromPage,
        int toPage
) {}
