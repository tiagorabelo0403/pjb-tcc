package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoErroClassificacaoView(
        Long jobId,
        int pagina,
        String detalhe
) {}
