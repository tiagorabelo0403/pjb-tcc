package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

public class AnthropicRetryableApiException extends AnthropicApiErrorException {

    public AnthropicRetryableApiException(int httpStatus, String errorType, String message) {
        super(httpStatus, errorType, message);
    }
}
