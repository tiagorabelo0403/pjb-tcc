package com.tcc.pjb.backend.service.processual.runtime.guard;

public class PjbOperationalGuardException extends RuntimeException {
    private final String code;

    public PjbOperationalGuardException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
