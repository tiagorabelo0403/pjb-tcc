package com.tcc.pjb.backend.ai.legalai.dreaming.domain;

public enum DreamStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELED;
    }

    public boolean isAtivo() {
        return this == PENDING || this == RUNNING;
    }
}
