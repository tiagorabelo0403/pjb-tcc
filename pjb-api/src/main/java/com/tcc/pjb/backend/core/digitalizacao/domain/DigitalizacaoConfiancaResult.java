package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoConfiancaResult(
        Long jobId,
        double confiancaMedia,
        boolean revisaoRequerida,
        int paginasComRevisao
) {}
