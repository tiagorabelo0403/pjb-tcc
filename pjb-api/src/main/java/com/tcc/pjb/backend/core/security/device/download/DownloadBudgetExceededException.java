package com.tcc.pjb.backend.core.security.device.download;

public class DownloadBudgetExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public DownloadBudgetExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
