package com.tcc.pjb.backend.core.audit.cross.service;

import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.cross.contract.CrossAuditSignal;

@Service
public class CrossAuditPublisher {

    private final ApplicationEventPublisher publisher;

    public CrossAuditPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(CrossAuditSignal signal) {
        if (signal == null) return;
        if (isBlank(signal.correlationKey()) || isBlank(signal.resourceType()) || isBlank(signal.resourceId())) {
            return;
        }
        publisher.publishEvent(signal);
    }

    public void publish(String correlationKey, String resourceType, String resourceId, String payloadHash) {
        Objects.requireNonNull(correlationKey, "correlationKey");
        publish(new CrossAuditSignal(correlationKey, resourceType, resourceId, payloadHash));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
