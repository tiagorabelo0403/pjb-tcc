package com.tcc.pjb.backend.configs.security;

public class PasskeyRequiredException extends RuntimeException {

    public PasskeyRequiredException(String message) {
        super(message);
    }
}
