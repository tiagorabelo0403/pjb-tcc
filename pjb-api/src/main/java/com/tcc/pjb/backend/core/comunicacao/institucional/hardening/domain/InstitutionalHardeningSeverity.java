package com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain;

public enum InstitutionalHardeningSeverity {
    INFO,
    WARN,
    ERROR;

    public boolean isBlocking() {
        return this == ERROR;
    }
}
