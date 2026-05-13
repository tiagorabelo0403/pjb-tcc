package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoProjectionAuditView(
        String reference,
        String status,
        String summary
) {
}
