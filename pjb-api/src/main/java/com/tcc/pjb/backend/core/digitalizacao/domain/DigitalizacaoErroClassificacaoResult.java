package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoErroClassificacaoResult(
        Long jobId,
        int pagina,
        String textoClassificado,
        String failureReason,
        boolean recoverable
) {}
