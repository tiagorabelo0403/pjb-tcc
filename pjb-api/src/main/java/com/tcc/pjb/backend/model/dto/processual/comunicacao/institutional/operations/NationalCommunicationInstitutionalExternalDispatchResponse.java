package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;

public record NationalCommunicationInstitutionalExternalDispatchResponse(
        String dispatchId,
        String jobId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String caixaCodigo,
        String destinatarioKind,
        String papelProcessual,
        String canal,
        String provider,
        String status,
        String providerReference,
        String payloadHash,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
