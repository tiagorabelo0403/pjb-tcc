package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoConfiancaView(
        Long jobId,
        double confiancaMedia,
        boolean revisaoRequerida
) {}
