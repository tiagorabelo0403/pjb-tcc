package com.tcc.pjb.backend.core.governance.idempotency;


public class IdempotencyInProgressException extends RuntimeException {

    private final String action;
    private final String requestHash;

    public IdempotencyInProgressException(String action, String requestHash) {
        super("idempotency in progress");
        this.action = action;
        this.requestHash = requestHash;
    }

    public String getAction() {
        return action;
    }

    public String getRequestHash() {
        return requestHash;
    }
}
