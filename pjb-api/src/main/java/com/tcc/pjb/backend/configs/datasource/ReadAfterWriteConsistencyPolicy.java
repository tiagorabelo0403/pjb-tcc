package com.tcc.pjb.backend.configs.datasource;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class ReadAfterWriteConsistencyPolicy {

    private static final String ATTR_KEY = "pjb.raw.write.at";

    private final Clock clock;
    private final long windowMs;

    public ReadAfterWriteConsistencyPolicy(
            Clock clock,
            @Value("${pjb.datasource.routing.read-your-writes-window:5s}") Duration readYourWritesWindow) {
        this.clock = clock;
        this.windowMs = readYourWritesWindow.toMillis();
    }

    public void markWrite() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(ATTR_KEY, clock.millis(), RequestAttributes.SCOPE_REQUEST);
        }
    }

    public boolean shouldForcePrimary() {
        Long writeAt = lastWriteAtEpochMillis();
        if (writeAt == null) {
            return false;
        }
        return (clock.millis() - writeAt) < windowMs;
    }

    public Long lastWriteAtEpochMillis() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object val = attrs.getAttribute(ATTR_KEY, RequestAttributes.SCOPE_REQUEST);
        if (!(val instanceof Long writeAt)) {
            return null;
        }
        return writeAt;
    }

    public long windowMillis() {
        return windowMs;
    }
}
