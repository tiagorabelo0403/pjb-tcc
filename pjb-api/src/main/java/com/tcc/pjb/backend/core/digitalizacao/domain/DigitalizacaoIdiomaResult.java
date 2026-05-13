package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoIdiomaResult(
        Long jobId,
        String idioma,
        String ocrEngine,
        boolean supported
) {}
