package com.tcc.pjb.backend.core.security.audit;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public final class PjbSecurityEventLogger {

    private static final Logger SECURITY = LoggerFactory.getLogger("PJB_SECURITY");
    private final MeterRegistry meterRegistry;

    public PjbSecurityEventLogger(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void bolaAttempt(String trackingCode, String userId, String outcome) {
        SECURITY.warn("BOLA_ATTEMPT correlationId={} trackingCode={} userId={} outcome={}",
                MDC.get("correlationId"), trackingCode, userId, outcome);
        meterRegistry.counter("pjb.security.bola.attempt", "outcome", outcome).increment();
    }

    public void stepUpDenied(String userId, String requiredLevel, String actualLevel) {
        SECURITY.warn("STEPUP_DENIED correlationId={} userId={} required={} actual={}",
                MDC.get("correlationId"), userId, requiredLevel, actualLevel);
        meterRegistry.counter("pjb.security.stepup.denied").increment();
    }

    public void rateLimitExceeded(String userId, String capability) {
        SECURITY.warn("RATE_LIMIT correlationId={} userId={} capability={}",
                MDC.get("correlationId"), userId, capability);
        meterRegistry.counter("pjb.security.ratelimit.exceeded", "capability", capability).increment();
    }

    public void idempotencyHit(String bodyHash, String existingTrackingCode) {
        SECURITY.info("IDEMPOTENCY_HIT correlationId={} bodyHash={} existingTrackingCode={}",
                MDC.get("correlationId"), bodyHash, existingTrackingCode);
        meterRegistry.counter("pjb.security.idempotency.hit").increment();
    }
}
