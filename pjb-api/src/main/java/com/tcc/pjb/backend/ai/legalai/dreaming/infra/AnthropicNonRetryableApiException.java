package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

public class AnthropicNonRetryableApiException extends AnthropicApiErrorException {

    public AnthropicNonRetryableApiException(int httpStatus, String errorType, String message) {
        super(httpStatus, errorType, message);
    }
}
