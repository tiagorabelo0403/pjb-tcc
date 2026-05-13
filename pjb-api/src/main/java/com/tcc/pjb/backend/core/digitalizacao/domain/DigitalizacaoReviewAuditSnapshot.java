package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoReviewAuditSnapshot(Long paginaId,
                                               boolean revisado,
                                               String tipoPeca,
                                               String status) {}
