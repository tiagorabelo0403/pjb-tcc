package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalDeliveryQueueItemResponse(
        String jobId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String caixaCodigo,
        String destinatarioKind,
        String papelProcessual,
        String canalAtual,
        String status,
        int attemptCount,
        int maxAttempts,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Instant terminalAt,
        String providerReference,
        String lastFailureReason,
        String lastError,
        List<String> justificativas,
        String hashIntegridade
) {
}
