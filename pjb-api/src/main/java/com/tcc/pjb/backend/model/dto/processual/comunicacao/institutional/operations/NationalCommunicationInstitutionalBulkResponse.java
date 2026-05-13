package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalBulkResponse(
        String operation,
        int totalRequested,
        int totalSucceeded,
        int totalFailed,
        List<String> expedicoesSucesso,
        List<String> failures,
        Instant processedAt
) {
}
