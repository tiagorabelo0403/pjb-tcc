package com.tcc.pjb.backend.integration.judicial.security;

public class JudicialConnectorCryptographicException extends RuntimeException {

    public JudicialConnectorCryptographicException(String message) {
        super(message);
    }

    public JudicialConnectorCryptographicException(String message, Throwable cause) {
        super(message, cause);
    }
}
