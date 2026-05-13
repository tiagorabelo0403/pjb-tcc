package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoAuditHealthView(
        String codigoTema,
        long sobrestados,
        long retomados,
        boolean healthy
) {}
