package com.tcc.pjb.backend.platform.runtime.execution;

import java.util.concurrent.RejectedExecutionException;

public final class PjbExecutionRejectedException extends RejectedExecutionException {

    public PjbExecutionRejectedException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
