package com.tcc.pjb.backend.core.comunicacao.institucional.integration.contract;

import java.time.Instant;

public record InstitutionalCommunicationContractEnvelope(
        String schema,
        String provider,
        String assinatura,
        Instant generatedAt,
        InstitutionalCommunicationContractPayload payload
) {
}
