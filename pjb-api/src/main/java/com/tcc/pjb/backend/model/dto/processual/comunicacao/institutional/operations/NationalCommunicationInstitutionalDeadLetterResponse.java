package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalDeadLetterResponse(
        String entryId,
        String jobId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String caixaCodigo,
        String channel,
        String reason,
        int attempts,
        String detail,
        List<String> justificativas,
        Instant createdAt,
        String hashIntegridade
) {
}
