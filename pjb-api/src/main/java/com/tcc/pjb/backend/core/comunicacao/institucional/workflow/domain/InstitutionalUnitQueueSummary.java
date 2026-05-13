package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.time.Instant;

public record InstitutionalUnitQueueSummary(
        String unidadeCodigo,
        String unidadeSigla,
        String caixaCodigo,
        long total,
        long disponibilizadas,
        long recebidas,
        long cientificadas,
        long cumpridas,
        long atrasadas,
        Instant prazoMaisProximo,
        Instant generatedAt
) {
}
