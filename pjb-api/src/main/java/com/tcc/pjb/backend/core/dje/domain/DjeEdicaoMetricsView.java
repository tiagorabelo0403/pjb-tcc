package com.tcc.pjb.backend.core.dje.domain;

public record DjeEdicaoMetricsView(
        String edicao,
        long publicacoes,
        long falhas,
        long notificacoesPendentes
) {}
