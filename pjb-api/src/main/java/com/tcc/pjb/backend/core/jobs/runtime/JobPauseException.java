package com.tcc.pjb.backend.core.jobs.runtime;

public class JobPauseException extends RuntimeException {

    private final String reason;

    public JobPauseException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
