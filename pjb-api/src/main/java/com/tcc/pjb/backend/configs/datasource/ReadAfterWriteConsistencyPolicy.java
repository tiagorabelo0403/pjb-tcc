package com.tcc.pjb.backend.configs.datasource;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class ReadAfterWriteConsistencyPolicy {

    private static final String ATTR_KEY = "pjb.raw.write.at";
    private static final long WINDOW_MS = 2_000L;

    public void markWrite() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(ATTR_KEY, Instant.now().toEpochMilli(), RequestAttributes.SCOPE_REQUEST);
        }
    }

    public boolean shouldForcePrimary() {
        Long writeAt = lastWriteAtEpochMillis();
        if (writeAt == null) {
            return false;
        }
        return (System.currentTimeMillis() - writeAt) < WINDOW_MS;
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
        return WINDOW_MS;
    }
}
