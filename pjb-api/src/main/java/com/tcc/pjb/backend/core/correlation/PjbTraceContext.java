package com.tcc.pjb.backend.core.correlation;

import java.util.Map;
import java.util.Objects;

public record PjbTraceContext(CorrelationId correlationId, CausationId causationId) {

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_CAUSATION_ID = "X-Causation-Id";

    public PjbTraceContext {
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(causationId, "causationId");
    }

    public static PjbTraceContext random() {
        return new PjbTraceContext(CorrelationId.random(), CausationId.random());
    }

    public static PjbTraceContext correlationOnly(CorrelationId correlationId) {
        return new PjbTraceContext(correlationId, CausationId.of(correlationId.value()));
    }

    public Map<String, String> asHeaders() {
        return Map.of(
                HEADER_CORRELATION_ID, correlationId.headerValue(),
                HEADER_CAUSATION_ID, causationId.headerValue()
        );
    }

    public PjbTraceContext next() {
        return new PjbTraceContext(correlationId, CausationId.random());
    }
}
