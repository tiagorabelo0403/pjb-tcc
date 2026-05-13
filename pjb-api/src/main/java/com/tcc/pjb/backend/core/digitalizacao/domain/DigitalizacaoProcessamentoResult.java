package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoProcessamentoResult(
        Long jobId,
        int totalPaginas,
        int paginasComRevisao,
        double confiancaMedia
) {
}
