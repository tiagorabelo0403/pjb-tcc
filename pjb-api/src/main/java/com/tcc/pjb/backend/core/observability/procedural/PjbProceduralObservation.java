package com.tcc.pjb.backend.core.observability.procedural;

import java.time.Instant;

public record PjbProceduralObservation(String processNumber,
                                       PjbProceduralObservationType type,
                                       String unitCode,
                                       int severity,
                                       Instant observedAt) {
}
