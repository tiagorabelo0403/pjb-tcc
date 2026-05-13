package com.tcc.pjb.backend.core.kernel.recursal.model;

import java.util.Objects;

public record MovementRecordedPayload(
        String movementCode,
        String movementText,
        String rawSource
) implements CanonicalFactPayload {

    public MovementRecordedPayload {
        movementCode = Objects.toString(movementCode, "").trim();
        movementText = Objects.toString(movementText, "").trim();
        rawSource = Objects.toString(rawSource, "").trim();
    }
}
