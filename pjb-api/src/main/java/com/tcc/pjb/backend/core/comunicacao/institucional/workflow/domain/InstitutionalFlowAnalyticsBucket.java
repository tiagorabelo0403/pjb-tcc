package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

public record InstitutionalFlowAnalyticsBucket(
        String dimensao,
        String valor,
        long total,
        double percentual,
        double mediaHoras
) {
}
