package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudEntryProjection(Long processoId,
                                     String numeroUnificado,
                                     String tribunal,
                                     String classeTpuCodigo) {
}
