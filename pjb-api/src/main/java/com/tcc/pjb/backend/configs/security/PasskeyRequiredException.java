package com.tcc.pjb.backend.configs.security;

public class PasskeyRequiredException extends RuntimeException {

    PasskeyRequiredException(String message) {
        super(message);
    }
}
