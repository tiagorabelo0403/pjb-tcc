package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoConfiancaQuery(
        Long jobId,
        double threshold
) {}
