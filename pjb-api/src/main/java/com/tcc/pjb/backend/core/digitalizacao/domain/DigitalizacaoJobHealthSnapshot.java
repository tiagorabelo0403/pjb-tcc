package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoJobHealthSnapshot(Long jobId,
                                             String status,
                                             boolean reviewRequired,
                                             double confiancaMedia) {}
