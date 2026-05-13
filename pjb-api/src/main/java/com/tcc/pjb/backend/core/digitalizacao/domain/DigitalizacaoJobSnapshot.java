package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoJobSnapshot(Long jobId,
                                       String status,
                                       int paginasProcessadas,
                                       double confiancaMedia) {
}
