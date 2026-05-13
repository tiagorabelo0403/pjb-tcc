package com.tcc.pjb.backend.core.prazos.auditoria.domain;

public record PrazoCalculationAuditView(
        Long processoId,
        String eventoRef,
        String regime,
        boolean consistent,
        String summary
) {}
