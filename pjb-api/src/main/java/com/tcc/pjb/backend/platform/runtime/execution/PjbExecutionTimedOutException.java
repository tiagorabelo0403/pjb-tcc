package com.tcc.pjb.backend.platform.runtime.execution;

public final class PjbExecutionTimedOutException extends RuntimeException {

    public PjbExecutionTimedOutException(String message) {
        super(message);
    }
}
