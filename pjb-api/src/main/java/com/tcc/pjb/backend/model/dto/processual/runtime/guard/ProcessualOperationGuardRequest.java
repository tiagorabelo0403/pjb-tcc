package com.tcc.pjb.backend.model.dto.processual.runtime.guard;

import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessualOperationGuardRequest(
        Long processoId,
        @NotBlank @Size(max = 80) String operationCode,
        @Size(max = 180) String idempotencyKey,
        @Size(max = 64) String payloadHash,
        @NotNull Boolean emitOutbox,
        @Size(max = 80) String aggregateType,
        @Size(max = 180) String aggregateId,
        Map<String, Object> metadata
) {
}
