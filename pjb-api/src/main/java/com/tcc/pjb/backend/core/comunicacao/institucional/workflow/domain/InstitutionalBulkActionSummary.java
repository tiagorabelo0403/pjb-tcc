package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalBulkActionSummary(
        String operation,
        int totalRequested,
        int totalSucceeded,
        int totalFailed,
        List<String> expedicoesSucesso,
        List<String> failures,
        Instant processedAt
) {
    public InstitutionalBulkActionSummary {
        operation = require(operation, "operation");
        expedicoesSucesso = PayloadMaps.copyTrimmedStrings(expedicoesSucesso);
        failures = PayloadMaps.copyTrimmedStrings(failures);
        Objects.requireNonNull(processedAt, "processedAt");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
