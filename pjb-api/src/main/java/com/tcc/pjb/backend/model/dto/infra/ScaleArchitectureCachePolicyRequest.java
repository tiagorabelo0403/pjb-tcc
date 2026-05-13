package com.tcc.pjb.backend.model.dto.infra;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ScaleArchitectureCachePolicyRequest(
        @NotBlank String cacheName,
        @NotBlank String roleName,
        @Min(1) int ttlSeconds,
        @Min(0) int staleWhileRevalidateSeconds,
        boolean enabled,
        String notes
) {
}
