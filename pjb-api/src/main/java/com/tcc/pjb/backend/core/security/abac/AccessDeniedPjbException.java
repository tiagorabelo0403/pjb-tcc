package com.tcc.pjb.backend.core.security.abac;


public class AccessDeniedPjbException extends SecurityException {

    public AccessDeniedPjbException(String message) {
        super(message);
    }
}
