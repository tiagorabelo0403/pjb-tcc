package com.tcc.pjb.backend.platform.logging;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;

public final class MdcTraceScope implements AutoCloseable {

    public static final String MDC_TRACE_ID = "trace_id";
    public static final String MDC_PHASE = "trace_phase";
    public static final String MDC_SPAN_ID = "trace_span";

    private final Map<String, String> previousValues;

    private MdcTraceScope(Map<String, String> previousValues) {
        this.previousValues = previousValues;
    }

    public static MdcTraceScope open(String traceId, String phase, String spanId) {
        Map<String, String> previousValues = capture();
        put(MDC_TRACE_ID, traceId);
        put(MDC_PHASE, phase);
        put(MDC_SPAN_ID, spanId);
        return new MdcTraceScope(previousValues);
    }

    @Override
    public void close() {
        restore(MDC_TRACE_ID);
        restore(MDC_PHASE);
        restore(MDC_SPAN_ID);
    }

    private static Map<String, String> capture() {
        Map<String, String> values = new HashMap<>();
        values.put(MDC_TRACE_ID, MDC.get(MDC_TRACE_ID));
        values.put(MDC_PHASE, MDC.get(MDC_PHASE));
        values.put(MDC_SPAN_ID, MDC.get(MDC_SPAN_ID));
        return values;
    }

    private static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, value.trim());
    }

    private void restore(String key) {
        String previous = previousValues.get(key);
        if (previous == null || previous.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, previous);
    }
}
