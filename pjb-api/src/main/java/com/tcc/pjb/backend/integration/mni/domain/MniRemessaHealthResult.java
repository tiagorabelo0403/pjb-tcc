package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaHealthResult(Long remessaId, String status, boolean confirmed, boolean payloadReady) {
    public boolean healthy() { return confirmed; }
}
