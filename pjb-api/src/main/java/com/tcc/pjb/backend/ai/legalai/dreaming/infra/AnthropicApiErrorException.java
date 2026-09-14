package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

public class AnthropicApiErrorException extends RuntimeException {

    private final int httpStatus;
    private final String errorType;

    public AnthropicApiErrorException(int httpStatus, String errorType, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorType = errorType;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String errorType() {
        return errorType;
    }
}
