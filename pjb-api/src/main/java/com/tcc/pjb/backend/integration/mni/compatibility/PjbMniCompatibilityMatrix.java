package com.tcc.pjb.backend.integration.mni.compatibility;

import java.util.List;
import java.util.Objects;

public record PjbMniCompatibilityMatrix(List<PjbMniTribunalCapability> capabilities) {
    public PjbMniCompatibilityMatrix {
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }

    public PjbMniCompatibilityLevel level(String tribunalCode, String operation) {
        String tribunal = Objects.toString(tribunalCode, "").trim().toUpperCase();
        String op = Objects.toString(operation, "").trim().toLowerCase();
        return capabilities.stream()
                .filter(capability -> capability.tribunalCode().equals(tribunal))
                .filter(capability -> capability.operation().equals(op))
                .map(PjbMniTribunalCapability::level)
                .findFirst()
                .orElse(PjbMniCompatibilityLevel.NOT_DECLARED);
    }

    public boolean ready(String tribunalCode, String operation) {
        PjbMniCompatibilityLevel level = level(tribunalCode, operation);
        return level == PjbMniCompatibilityLevel.VERIFIED || level == PjbMniCompatibilityLevel.DEGRADED;
    }
}
