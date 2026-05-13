package com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain;

public class InstitutionalConcurrentOperationException extends IllegalStateException {

    public InstitutionalConcurrentOperationException(String message) {
        super(message);
    }
}
