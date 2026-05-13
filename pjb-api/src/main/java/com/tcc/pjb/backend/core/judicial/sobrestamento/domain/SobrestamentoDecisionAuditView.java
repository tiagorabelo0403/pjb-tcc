package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoDecisionAuditView(
        String reference,
        String status,
        String summary
) {
}
