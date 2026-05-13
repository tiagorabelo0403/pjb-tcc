package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record IntegracaoJudicialEnvelopeView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
