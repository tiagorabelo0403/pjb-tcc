package com.tcc.pjb.backend.core.observability.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.tcc.pjb.backend.core.observability.RequestContext;
import java.util.Map;
import java.util.Optional;

public final class RequestIdConverter extends ClassicConverter {

    private static final String FALLBACK_REQUEST_ID = "-";
    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    public String convert(ILoggingEvent event) {
        return requestIdFromEvent(event)
                .or(this::requestIdFromContext)
                .filter(this::hasText)
                .orElse(FALLBACK_REQUEST_ID);
    }

    private Optional<String> requestIdFromEvent(ILoggingEvent event) {
        if (event == null) {
            return Optional.empty();
        }
        Map<String, String> properties = event.getMDCPropertyMap();
        if (properties == null || properties.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(properties.get(REQUEST_ID_KEY)).map(String::trim);
    }

    private Optional<String> requestIdFromContext() {
        return RequestContext.getRequestId().map(String::trim);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
