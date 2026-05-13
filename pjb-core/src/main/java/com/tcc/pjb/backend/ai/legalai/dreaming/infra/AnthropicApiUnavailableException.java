package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

public class AnthropicApiUnavailableException extends RuntimeException {

    public AnthropicApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
