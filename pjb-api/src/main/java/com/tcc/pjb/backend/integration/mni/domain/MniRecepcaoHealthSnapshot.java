package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoHealthSnapshot(
        Long recepcaoId,
        String status,
        boolean processado,
        boolean hasPayload
) {
    public boolean payloadHashPresent() { return hasPayload; }
    public boolean processed() { return processado; }
}
