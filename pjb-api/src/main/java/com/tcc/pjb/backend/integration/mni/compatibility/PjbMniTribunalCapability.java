package com.tcc.pjb.backend.integration.mni.compatibility;

import java.util.Objects;

public record PjbMniTribunalCapability(
        String tribunalCode,
        String operation,
        PjbMniCompatibilityLevel level,
        String evidence
) {
    public PjbMniTribunalCapability {
        tribunalCode = Objects.toString(tribunalCode, "").trim().toUpperCase();
        operation = Objects.toString(operation, "").trim().toLowerCase();
        level = level == null ? PjbMniCompatibilityLevel.NOT_DECLARED : level;
        evidence = Objects.toString(evidence, "").trim();
    }

    public boolean usable() {
        return level == PjbMniCompatibilityLevel.VERIFIED || level == PjbMniCompatibilityLevel.DEGRADED;
    }
}
