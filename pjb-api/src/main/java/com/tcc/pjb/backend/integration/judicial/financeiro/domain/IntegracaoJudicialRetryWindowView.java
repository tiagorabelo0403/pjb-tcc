package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record IntegracaoJudicialRetryWindowView(
        Long processoId,
        Instant nextRetryAt,
        int tentativas,
        String status
) {}
