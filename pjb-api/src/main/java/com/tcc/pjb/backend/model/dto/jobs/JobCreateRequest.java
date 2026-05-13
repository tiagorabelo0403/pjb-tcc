package com.tcc.pjb.backend.model.dto.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record JobCreateRequest(
        @NotBlank String type,
        String inboxKey,
        Integer priority,
        Integer maxAttempts,
        JsonNode input
) {
}
