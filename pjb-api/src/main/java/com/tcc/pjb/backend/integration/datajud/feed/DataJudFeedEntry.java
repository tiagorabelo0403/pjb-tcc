package com.tcc.pjb.backend.integration.datajud.feed;

public record DataJudFeedEntry(
        long processoId,
        String numeroUnificado,
        String tribunalCodigo,
        String classeTpuCodigo,
        String assunto,
        String statusProcesso
) {
}
