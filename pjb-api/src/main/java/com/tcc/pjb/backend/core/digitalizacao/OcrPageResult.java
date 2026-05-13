package com.tcc.pjb.backend.core.digitalizacao;

public record OcrPageResult(
        String texto,
        double confianca
) {
}
