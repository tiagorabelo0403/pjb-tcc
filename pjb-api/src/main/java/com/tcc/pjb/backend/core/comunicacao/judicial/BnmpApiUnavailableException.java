package com.tcc.pjb.backend.core.comunicacao.judicial;

public class BnmpApiUnavailableException extends RuntimeException {

    public BnmpApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
