package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoIdiomaView(
        Long jobId,
        String idioma,
        String engine
) {}
