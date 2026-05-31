package com.tcc.pjb.backend.core.guard;

import org.springframework.context.ApplicationEvent;

public final class MockGuardAuditEvent extends ApplicationEvent {

    private final MockGuardViolation violation;

    public MockGuardAuditEvent(Object source, MockGuardViolation violation) {
        super(source);
        this.violation = violation;
    }

    public MockGuardViolation getViolation() {
        return violation;
    }
}
