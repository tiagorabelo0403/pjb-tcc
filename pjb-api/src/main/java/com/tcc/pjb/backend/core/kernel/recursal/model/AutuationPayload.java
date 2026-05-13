package com.tcc.pjb.backend.core.kernel.recursal.model;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;

public record AutuationPayload(
        String targetProceedingNumber,
        InstanceLevel targetInstance,
        String targetCourt,
        String distributionUnit
) implements CanonicalFactPayload {

    public AutuationPayload {
        targetProceedingNumber = Objects.toString(targetProceedingNumber, "").trim();
        Objects.requireNonNull(targetInstance, "targetInstance é obrigatório");
        targetCourt = Objects.toString(targetCourt, "").trim();
        distributionUnit = Objects.toString(distributionUnit, "").trim();
    }
}
